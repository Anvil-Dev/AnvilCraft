package dev.dubhe.anvilcraft.integration.jei.transfer;

import mezz.jei.api.recipe.transfer.IRecipeTransferError;

import javax.annotation.Nullable;

/**
 * 重试原 Handler 传输逻辑的调用入口。各模组 Handler 的 mixin 通过 lambda 传入
 * "调用被注入的 transferRecipe 原方法"，使 {@link TerminalJeiTransferSupport}
 * 与具体 Handler 类解耦。
 */
@FunctionalInterface
public interface IRecipeTransferCallable {
    /**
     * 调用原 Handler 的传输方法（doTransfer=true）。
     *
     * @return 原方法的传输错误；null 表示成功
     */
    @Nullable IRecipeTransferError transfer();
}
