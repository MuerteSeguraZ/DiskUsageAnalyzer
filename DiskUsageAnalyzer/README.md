```markdown
# DiskUsageAnalyzer

**DiskUsageAnalyzer** is a Java Swing application that allows users to analyze disk usage, explore folders, manage files, and perform advanced operations such as compression, duplicate detection, and cleanup. It provides both a tree view and powerful tools to inspect and operate on your file system efficiently.

---

## 🔧 Features

### 📂 Folder Navigation
- Select and scan any folder using a file chooser dialog.
- Display folder structure in a `JTree` view.
- View total size, number of files, and folders.
- Refresh button to re-scan current folder.

### 🧰 Tools & Utilities

#### Compression Tools
- ZIP:
  - Extract ZIP here
  - Compress selected files/folders
- GZIP, TAR, BZIP2, XZ format support via utility classes.
  
#### File Operations
- **Checksum Calculator**: Compute SHA-256 for file integrity.
- **Find Duplicates**: Scan for duplicate files in a folder.
- **Disk Cleanup**: Delete large files over a size threshold.
- **Open Terminal**: Launch system terminal at selected folder location.

#### Compare Folders
- Context menu action `"Compare with..."`:
  - Select two folders.
  - Detect missing files, size differences, and extras.
  - View results in a dialog or comparison panel.

### 🆕 New File/Folder
- Right-click context menu for:
  - Creating new folders
  - Renaming
  - Deleting files/folders

### 📊 Visual Feedback
- `JProgressBar` shows scanning or long-running task progress.
- Status labels display current folder and statistics.

---

## 📁 Project Structure

DiskUsageAnalyzer/
│
├── DiskUsageAnalyzer.java           # Main UI and logic
├── pom.xml
├── utils/
│   ├── ZipUtils.java
│   ├── TarUtils.java
│   ├── BZip2Utils.java
│   ├── XZUtils.java

├── icons/                       # Icon

---

## 🚀 Getting Started

### ✅ Requirements
- Java 8 or higher
- Swing (bundled with Java SE)

### 🔨 Build & Run

### CLI:
```bash
javac DiskUsageAnalyzer/*.java utils/*.java
java DiskUsageAnalyzer.DiskUsageAnalyzer
```

#### Or use an IDE:
- Open the folder as a project.
- Compile and run `DiskUsageAnalyzer.java`.

---

## 🧠 TODO / Planned Features
- [ ] TreeMap-style visual disk usage view
- [ ] Export disk analysis to PDF/CSV
- [ ] Dark mode support
- [ ] Drag & drop file/folder operations

---

## 📜 License

MIT License – feel free to use, modify, and share.

---

## 🙌 Credits

Developed by MuerteSeguraZ  
Compression features powered by custom utility classes: `TarUtils`, `BZip2Utils`, `XZUtils`, etc.
```
