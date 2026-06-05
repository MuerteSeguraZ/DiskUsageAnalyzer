# DiskUsageAnalyzer

**DiskUsageAnalyzer** is a Java Swing application that allows people to analyze disk usage, explore folders, manage files, and do advanced stuff like compression, duplicate detection, and cleanup. It gives both a tree view and powerful tools to inspect and operate on your file system efficiently.

---

## Features

### Folder Navigation
- Select and scan any folder using a file chooser dialog.
- View total size, number of files, and folders.
- Refresh button to re-scan current folder.

### Tools & Utilities

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

### New File/Folder
- Right-click context menu for:
  - Creating new folders
  - Renaming
  - Deleting files/folders

### Visual Feedback
- `JProgressBar` shows scanning or long-running task progress.
- Status labels display current folder and statistics.

---

## Getting Started

### Build & Run

### CLI:
```bash
mvn compile
mvn exec:java "-Dexec.mainClass=DiskUsageAnalyzer.DiskUsageAnalyzer"
```

---

## TODO
- [x] TreeMap-style visual disk usage view
- [x] Export disk analysis to CSV/JSON/HTML
- [x] Export to PDF
- [ ] Dark mode support
- [ ] Drag & drop file/folder operations

---

## License

MIT

---

## Credits

Developed by MuerteSeguraZ  
PDFBox by Apache Software Foundation
Compression features powered by custom utility classes: `TarUtils`, `BZip2Utils`, `XZUtils`, etc.
