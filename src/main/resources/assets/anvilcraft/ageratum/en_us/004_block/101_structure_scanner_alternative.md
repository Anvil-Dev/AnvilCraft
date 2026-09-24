---
navigation:
  title: "§2Structure Scanner"
  icon: "anvilcraft:structure_scanner"
items:
  - anvilcraft:structure_scanner
  - anvilcraft:structure_disk
---

# Structure Disk

<row halign="center">
<recipe id="anvilcraft:disk_to_structure_disk"/>
<recipe id="anvilcraft:disk"/>
</row>

- Stores structures
- Can be used in the blueprint mode of the <ref item="anvilcraft:smart_block_placer"/>

> Sort of like a USB drive.

# Structure Scanner

<recipe id="anvilcraft:structure_scanner"/>

Used to scan and save structures.

## Manual Save

![structure_scanner.png](../../textures/structure_scanner.png)

Open the GUI:

1. Click the **right-side** button to start scanning the structure
2. Insert a <ref item="anvilcraft:structure_disk"/>
3. Enter a structure name (optional), and place a marker item to the right of the **Confirm** button (optional)
4. Click **Confirm Record Structure** to save

## Auto Save

1. <ref item="anvilcraft:structure_disk"/>s can be inserted and removed through chutes and other logistics blocks
2. Save when receiving a redstone signal

---

## Import and Export

1. Import and export operate on the same folder, located under the **world folder**. On servers, file operations require **operator permissions**
2. You can choose whether the structure can rotate automatically to prevent directional machines from being placed incorrectly

<tip>
Hold Shift and click the import/export button to open the folder
</tip>

<info>
These operations involve world storage (think of it as the cloud), <ref item="anvilcraft:structure_scanner"/> storage (think of it as your computer), and <ref item="anvilcraft:structure_disk"/> storage (think of it as a USB drive).
</info>

### Export (Upload) a Structure

#### Upload the contents of the <ref item="anvilcraft:structure_scanner"/> to the World

1. Scan a structure and save it to the scanner; its preview will appear on the right side of the screen
2. Enter a name for the exported file and click **Export Structure**

#### Upload the contents of a <ref item="anvilcraft:structure_disk"/> to the World

1. Ensure the scanner has no saved structure, or contains only scanned air
2. Insert a <ref item="anvilcraft:structure_disk"/> with a **recorded structure**
3. Enter a name for the exported file and click **Export Structure**

#### Download a Structure from the World

1. Enter the name of the file to import and click **Import Structure**; the imported structure is stored temporarily in the <ref item="anvilcraft:structure_scanner"/>
2. Insert a <ref item="anvilcraft:structure_disk"/> to save the structure from the <ref item="anvilcraft:structure_scanner"/> to the <ref item="anvilcraft:structure_disk"/>
