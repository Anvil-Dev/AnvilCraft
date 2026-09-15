package dev.dubhe.anvilcraft.api.number;

import com.mojang.serialization.DataResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * flat 表达式解析器，把 {@code x*2}、{@code 2x}、{@code 2$(cost)+1} 这样的文本解析为表达式树。
 *
 * <p>支持数字、{@code x}/{@code y}/{@code z}（等同于 {@code x0}/{@code x1}/{@code x2}）、
 * {@code $(name)} 具名传入值、四则运算（含 {@code ×} 与 {@code ÷}）、括号、一元正负号、
 * 隐式乘法（{@code 2x}、{@code 2(x+1)}）、乘方 {@code ^}，以及
 * {@code sqrt(x)}、{@code pow(x,2)}、{@code max(x,1)} 等函数调用。</p>
 */
public final class FlatExpressionParser {
    private static final Map<String, INumberExpression> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, FunctionExpression.Function> FUNCTIONS = Arrays
        .stream(FunctionExpression.Function.values())
        .collect(Collectors.toUnmodifiableMap(
            function -> function.getSerializedName().toLowerCase(Locale.ROOT),
            function -> function
        ));

    private final String source;
    private int position;

    private FlatExpressionParser(String source) {
        this.source = source;
    }

    /**
     * 解析 flat 表达式，解析结果会被缓存。
     *
     * @param source flat 表达式文本
     * @throws IllegalArgumentException 表达式不合法时抛出
     */
    public static INumberExpression parse(String source) {
        return CACHE.computeIfAbsent(source, key -> new FlatExpressionParser(key).parseWhole());
    }

    /**
     * 校验 flat 表达式，用于编解码器在配方加载阶段报错。
     */
    public static DataResult<String> validate(String source) {
        try {
            FlatExpressionParser.parse(source);
            return DataResult.success(source);
        } catch (RuntimeException exception) {
            return DataResult.error(() -> "Invalid expression: " + exception.getMessage());
        }
    }

    private INumberExpression parseWhole() {
        INumberExpression expression = this.parseAdditive();
        this.skipWhitespace();
        if (!this.atEnd()) throw this.error("unexpected character '" + this.peek() + "'");
        return expression;
    }

    private INumberExpression parseAdditive() {
        INumberExpression left = this.parseMultiplicative();
        while (true) {
            if (this.match('+')) {
                left = new ArithmeticExpression(ArithmeticExpression.Operator.ADD, left, this.parseMultiplicative());
                continue;
            }
            if (this.match('-')) {
                left = new ArithmeticExpression(ArithmeticExpression.Operator.SUBTRACT, left, this.parseMultiplicative());
                continue;
            }
            return left;
        }
    }

    private INumberExpression parseMultiplicative() {
        INumberExpression left = this.parseUnary();
        while (true) {
            if (this.match('*') || this.match('×')) {
                left = new ArithmeticExpression(ArithmeticExpression.Operator.MULTIPLY, left, this.parseUnary());
                continue;
            }
            if (this.match('/') || this.match('÷')) {
                left = new ArithmeticExpression(ArithmeticExpression.Operator.DIVIDE, left, this.parseUnary());
                continue;
            }
            if (!this.startsWithValue()) return left;
            left = new ArithmeticExpression(ArithmeticExpression.Operator.MULTIPLY, left, this.parseUnary());
        }
    }

    private INumberExpression parseUnary() {
        if (this.match('-')) {
            return new ArithmeticExpression(
                ArithmeticExpression.Operator.SUBTRACT,
                ConstantExpression.of(0),
                this.parseUnary()
            );
        }
        if (this.match('+')) return this.parseUnary();
        return this.parsePower();
    }

    private INumberExpression parsePower() {
        INumberExpression base = this.parseValue();
        if (this.match('^')) {
            return FunctionExpression.of(FunctionExpression.Function.POW, base, this.parseUnary());
        }
        return base;
    }

    private INumberExpression parseValue() {
        this.skipWhitespace();
        if (this.atEnd()) throw this.error("unexpected end of expression");
        char current = this.peek();
        if (current == '(') {
            this.position++;
            INumberExpression expression = this.parseAdditive();
            if (!this.match(')')) throw this.error("expected ')'");
            return expression;
        }
        if (current == '$') {
            this.position++;
            if (!this.match('(')) throw this.error("expected '(' after '$'");
            int start = this.position;
            while (!this.atEnd() && this.peek() != ')') {
                this.position++;
            }
            if (this.atEnd()) throw this.error("expected ')'");
            String name = this.source.substring(start, this.position).trim();
            this.position++;
            if (name.isEmpty()) throw this.error("expected a name between '$(' and ')'");
            return NamedExpression.of(name);
        }
        if (isDigit(current) || current == '.') return this.parseNumber();
        if (isIdentifierStart(current)) return this.parseIdentifier();
        throw this.error("unexpected character '" + current + "'");
    }

    private INumberExpression parseNumber() {
        final int start = this.position;
        while (!this.atEnd() && isDigit(this.peek())) {
            this.position++;
        }
        if (!this.atEnd() && this.peek() == '.') {
            do {
                this.position++;
            } while (!this.atEnd() && isDigit(this.peek()));
        }
        if (!this.atEnd() && (this.peek() == 'e' || this.peek() == 'E')) {
            int mark = this.position;
            this.position++;
            if (!this.atEnd() && (this.peek() == '+' || this.peek() == '-')) this.position++;
            if (!this.atEnd() && isDigit(this.peek())) {
                while (!this.atEnd() && isDigit(this.peek())) {
                    this.position++;
                }
            } else {
                this.position = mark;
            }
        }
        String text = this.source.substring(start, this.position);
        try {
            return ConstantExpression.of(Double.parseDouble(text));
        } catch (NumberFormatException exception) {
            throw this.error("invalid number '" + text + "'");
        }
    }

    private INumberExpression parseIdentifier() {
        int start = this.position;
        while (!this.atEnd() && isIdentifierPart(this.peek())) {
            this.position++;
        }
        String name = this.source.substring(start, this.position);
        String lower = name.toLowerCase(Locale.ROOT);
        INumberExpression variable = this.parseVariable(lower);
        if (variable != null) return variable;
        FunctionExpression.Function function = FUNCTIONS.get(lower);
        if (function == null) {
            throw this.error("unknown value '" + name + "', use $(name) to reference a named value");
        }
        if (!this.match('(')) throw this.error("expected '(' after function '" + name + "'");
        List<INumberExpression> arguments = new ArrayList<>();
        if (!this.match(')')) {
            while (true) {
                arguments.add(this.parseAdditive());
                if (this.match(',')) continue;
                if (this.match(')')) break;
                throw this.error("expected ',' or ')'");
            }
        }
        try {
            return new FunctionExpression(function, arguments);
        } catch (IllegalArgumentException exception) {
            throw this.error(exception.getMessage());
        }
    }

    /**
     * 解析 {@code x}/{@code y}/{@code z} 与 {@code x0}/{@code x1}/{@code x2} 形式的传入值引用。
     */
    @Nullable
    private INumberExpression parseVariable(String name) {
        switch (name) {
            case "x" -> {
                return InputExpression.of(0);
            }
            case "y" -> {
                return InputExpression.of(1);
            }
            case "z" -> {
                return InputExpression.of(2);
            }
            default -> {}
        }
        if (name.length() < 2 || name.charAt(0) != 'x') return null;
        String digits = name.substring(1);
        for (int index = 0; index < digits.length(); index++) {
            if (!isDigit(digits.charAt(index))) return null;
        }
        try {
            return InputExpression.of(Integer.parseInt(digits));
        } catch (NumberFormatException exception) {
            throw this.error("input index is too large: '" + digits + "'");
        }
    }

    /**
     * 下一个值是否能直接接在左侧，用于识别隐式乘法。
     */
    private boolean startsWithValue() {
        this.skipWhitespace();
        if (this.atEnd()) return false;
        char current = this.peek();
        return current == '(' || current == '$' || isIdentifierStart(current);
    }

    private boolean match(char character) {
        this.skipWhitespace();
        if (this.atEnd() || this.peek() != character) return false;
        this.position++;
        return true;
    }

    private void skipWhitespace() {
        while (!this.atEnd() && Character.isWhitespace(this.peek())) {
            this.position++;
        }
    }

    private char peek() {
        return this.source.charAt(this.position);
    }

    private boolean atEnd() {
        return this.position >= this.source.length();
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(
            message + " at position " + this.position + " of expression \"" + this.source + "\""
        );
    }

    private static boolean isDigit(char character) {
        return character >= '0' && character <= '9';
    }

    private static boolean isIdentifierStart(char character) {
        return character == '_' || (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z');
    }

    private static boolean isIdentifierPart(char character) {
        return isIdentifierStart(character) || isDigit(character);
    }
}
