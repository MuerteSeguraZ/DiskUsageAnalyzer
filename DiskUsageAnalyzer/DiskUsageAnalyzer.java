package DiskUsageAnalyzer;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.datatransfer.StringSelection;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.ArrayList;
import java.io.*;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;
import DiskUsageAnalyzer.utils.TarUtils;
import DiskUsageAnalyzer.utils.BZip2Utils;
import DiskUsageAnalyzer.utils.XZUtils;

@SuppressWarnings("unused")
public class DiskUsageAnalyzer extends JFrame {
    private JButton selectFolderButton;
    private JButton refreshButton;
    private JButton exportButton;
    private JButton seeAllButton;
    private JLabel folderPathLabel;
    private JLabel totalFilesLabel;
    private JLabel totalFoldersLabel;
    private JLabel totalSizeLabel;
    private JTree folderTree;
    private DefaultTreeModel treeModel;
    private JProgressBar progressBar;

    private int totalFiles;
    private int totalFolders;
    private long totalSize;
    private File currentFolder;
    private boolean seeAllEnabled = false;

    public DiskUsageAnalyzer() {
        setTitle("Disk Usage Analyzer");
        setSize(700, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        ImageIcon icon = new ImageIcon("C:/Users/Eudald/Desktop/java/DiskUsageAnalyzer/icon/icon_diskusage.png");
        setIconImage(icon.getImage());


        selectFolderButton = new JButton("Choose Folder");
        refreshButton = new JButton("Refresh");
        exportButton = new JButton("Export");
        seeAllButton = new JButton("See All");
        seeAllButton.setBackground(Color.RED); // Set initial color to red
        seeAllButton.addActionListener(e -> toggleSeeAll());

        folderPathLabel = new JLabel("No folder selected");

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("No Data");
        treeModel = new DefaultTreeModel(root);
        folderTree = new JTree(treeModel);

        totalFilesLabel = new JLabel("Total Files: 0");
        totalFoldersLabel = new JLabel("Total Folders: 0");
        totalSizeLabel = new JLabel("Total Size: 0 B");

        JScrollPane treeScroll = new JScrollPane(folderTree);
        progressBar = new JProgressBar();
        progressBar.setVisible(false);

        selectFolderButton.addActionListener(e -> selectFolder());
        refreshButton.addActionListener(e -> {
            if (currentFolder != null) {
                scanFolder(currentFolder);
            }
        });
        exportButton.addActionListener(e -> {
            if (currentFolder != null) {
                showExportDialog();
            } else {
                JOptionPane.showMessageDialog(this, "Select a folder first!");
            }
        });

        folderTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    int row = folderTree.getClosestRowForLocation(e.getX(), e.getY());
                    folderTree.setSelectionRow(row);
                    Object selectedNode = folderTree.getSelectionPath().getLastPathComponent();
                    if (selectedNode instanceof DefaultMutableTreeNode node) {
                        String nodeName = node.toString();
                        File nodeFile = getFileFromNodePath(node);
                        if (nodeFile != null) {
                            TreePopupMenu menu = new TreePopupMenu(nodeName, nodeFile, node);
                            menu.show(folderTree, e.getX(), e.getY());
                        }
                    }
                }
            }
        });

        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(selectFolderButton);
        topPanel.add(refreshButton);
        topPanel.add(exportButton);
        topPanel.add(seeAllButton); // Add the "See All" button
        topPanel.add(folderPathLabel);

        JPanel summaryPanel = new JPanel(new FlowLayout());
        summaryPanel.add(totalFilesLabel);
        summaryPanel.add(totalFoldersLabel);
        summaryPanel.add(totalSizeLabel);

        add(topPanel, BorderLayout.NORTH);
        add(treeScroll, BorderLayout.CENTER);
        add(progressBar, BorderLayout.SOUTH);
        add(summaryPanel, BorderLayout.PAGE_END);
    }

    private void toggleSeeAll() {
        seeAllEnabled = !seeAllEnabled; // Toggle the state
        seeAllButton.setBackground(seeAllEnabled ? Color.GREEN : Color.RED); // Change color
    }

    private void selectFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(seeAllEnabled ? JFileChooser.FILES_AND_DIRECTORIES : JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(true); // Show all files
        chooser.setDialogTitle("Select a Folder or File");

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentFolder = chooser.getSelectedFile();
            scanFolder(currentFolder);
        }
    }

    private void scanFolder(File folder) {
        folderPathLabel.setText(folder.getAbsolutePath());
        totalFiles = 0;
        totalFolders = 0;
        totalSize = 0;
        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(folder.getName() + " (" + readableSize(folderSize(folder)) + ")");
                buildTree(folder, rootNode);
                treeModel.setRoot(rootNode);
                return null;
            }

            @Override
            protected void done() {
                updateSummary();
                progressBar.setVisible(false);
            }
        };
        worker.execute();
    }

    private void buildTree(File folder, DefaultMutableTreeNode node) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                totalFolders++;
                long size = folderSize(file);
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(file.getName() + " (" + readableSize(size) + ")");
                node.add(childNode);
                buildTree(file, childNode);
            } else {
                node.add(new DefaultMutableTreeNode(file.getName() + " (" + readableSize(file.length()) + ")"));
                totalFiles++;
                totalSize += file.length();
            }
        }
    }

    private long folderSize(File folder) {
        long total = 0;
        File[] files = folder.listFiles();
        if (files == null) return 0;

        for (File file : files) {
            if (file.isFile()) {
                total += file.length();
            } else {
                total += folderSize(file);
            }
        }
        return total;
    }

    private String readableSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private void updateSummary() {
        totalFilesLabel.setText("Total Files: " + totalFiles);
        totalFoldersLabel.setText("Total Folders: " + totalFolders);
        totalSizeLabel.setText("Total Size: " + readableSize(totalSize));
    }

    private class TreePopupMenu extends JPopupMenu {
        public TreePopupMenu(String nodeName, File file, DefaultMutableTreeNode node) {
            File currentDirectory = file.isDirectory() ? file : file.getParentFile();
            JMenuItem infoItem = new JMenuItem("Properties");
            infoItem.addActionListener(e -> JOptionPane.showMessageDialog(null, "Node: " + nodeName, "Properties", JOptionPane.INFORMATION_MESSAGE));
            add(infoItem);

            JMenuItem openItem = new JMenuItem("Open in Explorer");
            openItem.addActionListener(e -> {
                try {
                    if (file.exists()) {
                        Desktop.getDesktop().open(file);
                    } else {
                        JOptionPane.showMessageDialog(null, "File or folder does not exist.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "Failed to open: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            add(openItem);

            JMenuItem copyPathItem = new JMenuItem("Copy Path");
            copyPathItem.addActionListener(e -> {
                StringSelection selection = new StringSelection(file.getAbsolutePath());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            });
            add(copyPathItem);

            JMenuItem compareWithItem = new JMenuItem("Compare with...");
            compareWithItem.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int result = chooser.showOpenDialog(DiskUsageAnalyzer.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File otherFolder = chooser.getSelectedFile();
                    try {
                        compareFolders(file, otherFolder);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, "Error comparing folders:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            add(compareWithItem);

            JMenu zipUtilitiesMenu = new JMenu("ZIP Utilities");

            JMenu zipMenu = new JMenu("ZIP");
            JMenu tarMenu = new JMenu("TAR");
            JMenu Bzip2Menu = new JMenu("Bzip2");
            JMenu XZMenu = new JMenu("XZ");
            JMenu toolsMenu = new JMenu("Tools");
            JMenu newMenu = new JMenu("New");

JMenuItem checksumItem = new JMenuItem("Calculate Checksum");
checksumItem.addActionListener(e -> {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        File selectedFile = chooser.getSelectedFile();
        try {
            String checksum = calculateSHA256(selectedFile);  // You implement this
            JOptionPane.showMessageDialog(null, 
                "SHA-256: " + checksum, "Checksum", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
});
toolsMenu.add(checksumItem);

JMenuItem findDuplicatesItem = new JMenuItem("Find Duplicates");
findDuplicatesItem.addActionListener(e -> {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        File folder = chooser.getSelectedFile();
        try {
            List<List<File>> duplicates = findDuplicatesInFolder(folder); // You implement this
            if (duplicates.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No duplicates found.");
            } else {
                // Just show filenames in dialog, customize as you want
                StringBuilder sb = new StringBuilder("Duplicates found:\n");
                for (List<File> group : duplicates) {
                    for (File f : group) sb.append(f.getName()).append("\n");
                    sb.append("\n");
                }
                JOptionPane.showMessageDialog(null, sb.toString());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
});
toolsMenu.add(findDuplicatesItem);

JMenuItem diskCleanupItem = new JMenuItem("Disk Cleanup");
diskCleanupItem.addActionListener(e -> {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        File folder = chooser.getSelectedFile();
        try {
            List<File> largeFiles = findLargeFiles(folder, 100 * 1024 * 1024); // >100MB, you implement
            if (largeFiles.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No large files (>100MB) found.");
            } else {
                int confirm = JOptionPane.showConfirmDialog(null,
                    "Delete these large files?\n" + largeFiles.stream().map(File::getName).reduce((a,b) -> a + "\n" + b).orElse(""),
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    int deletedCount = 0;
                    for (File f : largeFiles) {
                        if (f.delete()) deletedCount++;
                    }
                    JOptionPane.showMessageDialog(null, "Deleted " + deletedCount + " files.");
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
});
toolsMenu.add(diskCleanupItem);

JMenuItem openTerminalItem = new JMenuItem("Open Terminal Here");
openTerminalItem.addActionListener(e -> {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        File folder = chooser.getSelectedFile();
        try {
            openTerminalAt(folder); // You implement cross-platform here
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Failed to open terminal: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
});
toolsMenu.add(openTerminalItem);

            JMenuItem bzip2CompressItem = new JMenuItem("Compress to BZIP2");
bzip2CompressItem.addActionListener(e -> {
    JFileChooser chooser = new JFileChooser();
    chooser.setSelectedFile(new File(file.getName() + ".bz2"));
    int choice = chooser.showSaveDialog(null);
    if (choice == JFileChooser.APPROVE_OPTION) {
        try {
            BZip2Utils.compressBzip2(file, chooser.getSelectedFile());
            JOptionPane.showMessageDialog(null, "Compressed to: " + chooser.getSelectedFile().getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Error compressing file:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
});
Bzip2Menu.add(bzip2CompressItem);

JMenuItem bzip2DecompressItem = new JMenuItem("Extract BZIP2");
bzip2DecompressItem.addActionListener(e -> {
    JFileChooser chooser = new JFileChooser();
    chooser.setSelectedFile(new File(file.getName().replaceAll("\\.bz2$", "")));
    int choice = chooser.showSaveDialog(null);
    if (choice == JFileChooser.APPROVE_OPTION) {
        try {
            BZip2Utils.decompressBzip2(file, chooser.getSelectedFile());
            JOptionPane.showMessageDialog(null, "Decompressed to: " + chooser.getSelectedFile().getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Error decompressing file:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
});
Bzip2Menu.add(bzip2DecompressItem);

JMenuItem bzip2DecompressHereItem = new JMenuItem("Extract Here");
bzip2DecompressHereItem.addActionListener(e -> {
    try {
        File target = new File(file.getParentFile(), file.getName().replaceAll("\\.bz2$", ""));
        BZip2Utils.decompressBzip2(file, target);
        JOptionPane.showMessageDialog(null, "Decompressed to: " + target.getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
    } catch (IOException ex) {
        JOptionPane.showMessageDialog(null, "Error decompressing file:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
});
Bzip2Menu.add(bzip2DecompressHereItem);

JMenuItem xzCompressItem = new JMenuItem("Compress to XZ");
xzCompressItem.addActionListener(e -> {
    JFileChooser chooser = new JFileChooser();
    chooser.setSelectedFile(new File(file.getName() + ".xz"));
    int choice = chooser.showSaveDialog(null);
    if (choice == JFileChooser.APPROVE_OPTION) {
        try {
            XZUtils.compressXZ(file, chooser.getSelectedFile());
            JOptionPane.showMessageDialog(null, "Compressed to: " + chooser.getSelectedFile().getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Error compressing file:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
});
XZMenu.add(xzCompressItem);

JMenuItem xzDecompressItem = new JMenuItem("Extract XZ");
xzDecompressItem.addActionListener(e -> {
    JFileChooser chooser = new JFileChooser();
    chooser.setSelectedFile(new File(file.getName().replaceAll("\\.xz$", "")));
    int choice = chooser.showSaveDialog(null);
    if (choice == JFileChooser.APPROVE_OPTION) {
        try {
            XZUtils.decompressXZ(file, chooser.getSelectedFile());
            JOptionPane.showMessageDialog(null, "Decompressed to: " + chooser.getSelectedFile().getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Error decompressing file:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
});
XZMenu.add(xzDecompressItem);

JMenuItem xzExtractHereItem = new JMenuItem("Extract XZ Here");
xzExtractHereItem.addActionListener(e -> {
    File xzFile = file; // The selected .xz file
    if (!xzFile.getName().endsWith(".xz")) {
        JOptionPane.showMessageDialog(null, "Selected file is not an XZ archive.", "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }

    File target = new File(xzFile.getParentFile(), xzFile.getName().substring(0, xzFile.getName().length() - 3));
    try {
        XZUtils.decompressXZ(xzFile, target);
        JOptionPane.showMessageDialog(null, "Extracted to: " + target.getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
    } catch (IOException ex) {
        JOptionPane.showMessageDialog(null, "Error extracting file:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
});
XZMenu.add(xzExtractHereItem);


            // ZIP options
            JMenuItem zipItem = new JMenuItem("Compress to ZIP");
            zipItem.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setSelectedFile(new File(file.getName() + ".zip"));
                int choice = chooser.showSaveDialog(null);
                if (choice == JFileChooser.APPROVE_OPTION) {
                    try {
                        zipFolder(file, chooser.getSelectedFile());
                        JOptionPane.showMessageDialog(null, "Compressed to: " + chooser.getSelectedFile().getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(null, "Error compressing folder:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            zipMenu.add(zipItem);

            // Add "Compress ZIP Here" option
            JMenuItem compressZipHereItem = new JMenuItem("Compress ZIP Here");
            compressZipHereItem.addActionListener(e -> {
                try {
                    // Create the ZIP file in the same directory as the selected file/folder
                    File zipFile = new File(file.getParentFile(), file.getName() + ".zip"); // Add .zip extension
                    zipFolder(file, zipFile);
                    JOptionPane.showMessageDialog(null, "Compressed to: " + zipFile.getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
                    scanFolder(currentFolder); // Refresh tree
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "Error compressing folder:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            zipMenu.add(compressZipHereItem);

            if (file.isFile() && file.getName().toLowerCase().endsWith(".zip")) {
                JMenuItem unzipItem = new JMenuItem("Extract ZIP");
                unzipItem.addActionListener(e -> {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    chooser.setDialogTitle("Select Destination Folder");
                    int choice = chooser.showOpenDialog(null);
                    if (choice == JFileChooser.APPROVE_OPTION) {
                        try {
                            unzipFile(file, chooser.getSelectedFile());
                            JOptionPane.showMessageDialog(null, "Extracted to: " + chooser.getSelectedFile().getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(null, "Error extracting ZIP:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
                zipMenu.add(unzipItem);
            }

            JMenuItem extractZipHereItem = new JMenuItem("Extract ZIP here");
            extractZipHereItem.addActionListener(e -> {
                try {
                    unzipFile(file, file.getParentFile());
                    JOptionPane.showMessageDialog(null, "Extracted to: " + file.getParentFile().getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
                    scanFolder(currentFolder);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "Error extracting ZIP:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            zipMenu.add(extractZipHereItem);

            // GZIP options
            JMenu gzipMenu = new JMenu("GZIP");
            JMenuItem compressGzipItem = new JMenuItem("Compress to GZIP");
            compressGzipItem.addActionListener(e -> {
                try {
                    compressGzipFile(file);
                    JOptionPane.showMessageDialog(null, "Compressed: " + file.getName() + ".gz");
                    scanFolder(currentFolder); // Refresh tree
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "Failed to compress: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            gzipMenu.add(compressGzipItem);

            JMenuItem extractGzipItem = new JMenuItem("Extract GZIP");
            extractGzipItem.addActionListener(e -> {
                try {
                    decompressGzipFile(file);
                    JOptionPane.showMessageDialog(null, "Extracted: " + file.getName());
                    scanFolder(currentFolder); // Refresh tree
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "Failed to extract: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            gzipMenu.add(extractGzipItem);

            JMenuItem extractGzipHereItem = new JMenuItem("Extract GZIP here");
            extractGzipHereItem.addActionListener(e -> {
                try {
                    decompressGzipFile(file);
                    JOptionPane.showMessageDialog(null, "Extracted here: " + file.getName());
                    scanFolder(currentFolder); // Refresh tree
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "Failed to extract: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            gzipMenu.add(extractGzipHereItem);

            JMenuItem compressTarItem = new JMenuItem("Compress to TAR");
compressTarItem.addActionListener(e -> {
    JFileChooser chooser = new JFileChooser();
    chooser.setSelectedFile(new File(file.getName() + ".tar"));
    int choice = chooser.showSaveDialog(null);
    if (choice == JFileChooser.APPROVE_OPTION) {
        try {
            TarUtils.compressToTar(file, chooser.getSelectedFile());
            JOptionPane.showMessageDialog(null, "Compressed to: " + chooser.getSelectedFile().getName());
            scanFolder(currentFolder); // Refresh tree
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Failed to compress: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
});
tarMenu.add(compressTarItem);

JMenuItem extractTarItem = new JMenuItem("Extract TAR");
extractTarItem.addActionListener(e -> {
    try {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int choice = chooser.showOpenDialog(null);
        if (choice == JFileChooser.APPROVE_OPTION) {
            TarUtils.extractTar(file, chooser.getSelectedFile());
            JOptionPane.showMessageDialog(null, "Extracted: " + file.getName());
            scanFolder(currentFolder); // Refresh tree
        }
    } catch (IOException ex) {
        JOptionPane.showMessageDialog(null, "Failed to extract: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
});
tarMenu.add(extractTarItem);

JMenuItem extractTarHereItem = new JMenuItem("Extract TAR here");
extractTarHereItem.addActionListener(e -> {
    try {
        TarUtils.extractTar(file, file.getParentFile());
        JOptionPane.showMessageDialog(null, "Extracted here: " + file.getName());
        scanFolder(currentFolder); // Refresh tree
    } catch (IOException ex) {
        JOptionPane.showMessageDialog(null, "Failed to extract here: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
});
tarMenu.add(extractTarHereItem);

JMenuItem newFolder = new JMenuItem("Folder");
newFolder.addActionListener(e -> {
    String name = JOptionPane.showInputDialog(null, "Enter folder name:", "New Folder", JOptionPane.PLAIN_MESSAGE);
    if (name != null && !name.isBlank()) {
        File folder = new File(currentDirectory, name);
        if (!folder.mkdir()) {
            JOptionPane.showMessageDialog(null, "Failed to create folder!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
});

JMenuItem newFile = new JMenuItem("File");
newFile.addActionListener(e -> {
    String name = JOptionPane.showInputDialog(null, "Enter file name:", "New File", JOptionPane.PLAIN_MESSAGE);
    if (name != null && !name.isBlank()) {
        File Createfile = new File(currentDirectory, name);
        try {
            if (!file.createNewFile()) {
                JOptionPane.showMessageDialog(null, "Failed to create file!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
});

JMenuItem newTextDoc = new JMenuItem("Text Document (.txt)");
newTextDoc.addActionListener(e -> {
    String name = JOptionPane.showInputDialog(null, "Enter text document name:", "New Text Document", JOptionPane.PLAIN_MESSAGE);
    if (name != null && !name.isBlank()) {
        File createTXTFile = new File(currentDirectory, name.endsWith(".txt") ? name : name + ".txt");
        try {
            if (!file.createNewFile()) {
                JOptionPane.showMessageDialog(null, "Failed to create text document!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
});

JMenuItem newMarkdown = new JMenuItem("Markdown File (.md)");
newMarkdown.addActionListener(e -> {
    String name = JOptionPane.showInputDialog(null, "Enter markdown file name:", "New Markdown File", JOptionPane.PLAIN_MESSAGE);
    if (name != null && !name.isBlank()) {
        File createMarkdownFile = new File(currentDirectory, name.endsWith(".md") ? name : name + ".md");
        try {
            if (!file.createNewFile()) {
                JOptionPane.showMessageDialog(null, "Failed to create markdown file!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
});

JMenuItem newLogFile = new JMenuItem("Log File (.log)");
newLogFile.addActionListener(e -> {
    String name = JOptionPane.showInputDialog(null, "Enter log file name:", "New Log File", JOptionPane.PLAIN_MESSAGE);
    if (name != null && !name.isBlank()) {
        File createLogFile = new File(currentDirectory, name.endsWith(".log") ? name : name + ".log");
        try {
            if (!file.createNewFile()) {
                JOptionPane.showMessageDialog(null, "Failed to create log file!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
});

JMenuItem renameFile = new JMenuItem("Rename File");
renameFile.addActionListener(e -> {
    TreePath selectedPath = folderTree.getSelectionPath();
    if (selectedPath != null) {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
        File selectedFile = getFileFromNodePath(selectedNode); // Use your method to get the File
        String newName = JOptionPane.showInputDialog(null, "Enter new file name:", "Rename File", JOptionPane.PLAIN_MESSAGE);
        if (newName != null && !newName.isBlank()) {
            File renamedFile = new File(selectedFile.getParentFile(), newName);
            if (!selectedFile.renameTo(renamedFile)) {
                JOptionPane.showMessageDialog(null, "Failed to rename file!", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                // Optional: refresh the tree model
                scanFolder(currentFolder); // Refresh the tree to reflect changes
            }
        }
    } else {
        JOptionPane.showMessageDialog(null, "No file selected to rename.", "Warning", JOptionPane.WARNING_MESSAGE);
    }
});
add(renameFile);


newMenu.add(newFolder);
newMenu.add(newFile);
newMenu.add(newTextDoc);
newMenu.add(newMarkdown);
newMenu.add(newLogFile);



            // Add the GZIP menu to the ZIP Utilities menu
            zipUtilitiesMenu.add(gzipMenu);
            zipUtilitiesMenu.add(zipMenu);
            zipUtilitiesMenu.add(tarMenu);
            zipUtilitiesMenu.add(Bzip2Menu);
            zipUtilitiesMenu.add(XZMenu);
            add(zipUtilitiesMenu);
            add(toolsMenu);
            add(newMenu);

            JMenuItem deleteItem = new JMenuItem("Delete");
            if (node.getParent() == null) {
                deleteItem.setEnabled(false); // Disable delete on root node
            }
            deleteItem.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete " + file.getName() + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    deleteFolder(file);
                    if (node.getParent() != null) {
                        ((DefaultMutableTreeNode) node.getParent()).remove(node);
                        treeModel.reload();
                        updateSummary();
                    } else {
                        JOptionPane.showMessageDialog(null, "Cannot delete the root folder.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            add(deleteItem);
        }
    }

    private void showExportDialog() {
        JDialog dialog = new JDialog(this, "Export Format", true);
        dialog.setSize(300, 150);
        dialog.setLayout(new FlowLayout());
        dialog.setLocationRelativeTo(this);

        JButton csvButton = new JButton("Export as CSV");
        JButton jsonButton = new JButton("Export as JSON");
        JButton htmlButton = new JButton("Export as HTML");

        csvButton.addActionListener(e -> {
            exportAsCSV();
            dialog.dispose();
        });

        jsonButton.addActionListener(e -> {
            exportAsJSON();
            dialog.dispose();
        });

        htmlButton.addActionListener(e -> {
            exportAsHTML();
            dialog.dispose();
        });

        dialog.add(csvButton);
        dialog.add(jsonButton);
        dialog.add(htmlButton);
        dialog.setVisible(true);
    }

    private void exportAsCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("DiskUsageReport.csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(chooser.getSelectedFile())) {
                pw.println("Name,Type,Size (Bytes)");
                writeFolderToCSV(currentFolder, pw, "");
                JOptionPane.showMessageDialog(this, "CSV report saved successfully.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error saving CSV: " + ex.getMessage());
            }
        }
    }

    private void writeFolderToCSV(File folder, PrintWriter pw, String indent) {
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                pw.println(indent + "\"" + file.getName() + "\",Folder," + folderSize(file));
                writeFolderToCSV(file, pw, indent);
            } else {
                pw.println(indent + "\"" + file.getName() + "\",File," + file.length());
            }
        }
    }

    private void exportAsJSON() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("DiskUsageReport.json"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(chooser.getSelectedFile())) {
                Map<String, Object> jsonMap = buildJsonTree(currentFolder);
                pw.println(toJson(jsonMap, 0));
                JOptionPane.showMessageDialog(this, "JSON report saved successfully.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error saving JSON: " + ex.getMessage());
            }
        }
    }

    private Map<String, Object> buildJsonTree(File folder) {
        Map<String, Object> map = new LinkedHashMap<>();
        File[] files = folder.listFiles();
        if (files == null) return map;
        for (File file : files) {
            if (file.isDirectory()) {
                map.put(file.getName(), buildJsonTree(file));
            } else {
                map.put(file.getName(), file.length());
            }
        }
        return map;
    }

    private String toJson(Object obj, int indent) {
        StringBuilder sb = new StringBuilder();
        String ind = "  ".repeat(indent);
        if (obj instanceof Map) {
            sb.append("{\n");
            Map<?, ?> map = (Map<?, ?>) obj;
            int i = 0;
            for (var entry : map.entrySet()) {
                sb.append(ind).append("  \"").append(entry.getKey()).append("\": ");
                sb.append(toJson(entry.getValue(), indent + 1));
                if (i++ < map.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append(ind).append("}");
        } else if (obj instanceof Number) {
            sb.append(obj);
        } else {
            sb.append("\"").append(obj.toString()).append("\"");
        }
        return sb.toString();
    }

    private void exportAsHTML() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("DiskUsageReport.html"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(chooser.getSelectedFile())) {
                pw.println("<html><head><title>Disk Usage Report</title></head><body>");
                pw.println("< h1>Disk Usage Report</h1>");
                writeFolderToHTML(currentFolder, pw, 0);
                pw.println("</body></html>");
                JOptionPane.showMessageDialog(this, "HTML report saved successfully.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error saving HTML: " + ex.getMessage());
            }
        }
    }

    private void writeFolderToHTML(File folder, PrintWriter pw, int level) {
        String indent = "&nbsp;".repeat(level * 4);
        pw.println(indent + "<b>" + folder.getName() + "</b><br>");
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                writeFolderToHTML(file, pw, level + 1);
            } else {
                pw.println(indent + "&nbsp;&nbsp;" + file.getName() + " - " + file.length() + " bytes<br>");
            }
        }
    }

    private void deleteFolder(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteFolder(file);
                }
            }
        }
        folder.delete();
    }

    private File getFileFromNodePath(DefaultMutableTreeNode node) {
        StringBuilder path = new StringBuilder(currentFolder.getAbsolutePath());
        TreeNode[] nodes = node.getPath();
        for (int i = 1; i < nodes.length; i++) { // skip root
            String nodeName = nodes[i].toString();
            nodeName = nodeName.replaceAll(" \\(.*\\)$", ""); // Remove size text
            path.append(File.separator).append(nodeName);
        }
        return new File(path.toString());
    }

    private void zipFolder(File sourceFolder, File zipFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            zipFileRecursive(sourceFolder, sourceFolder.getName(), zos);
        }
    }

    private void zipFileRecursive(File fileToZip, String zipEntryName, ZipOutputStream zos) throws IOException {
        if (fileToZip.isHidden()) return;

        if (fileToZip.isDirectory()) {
            if (!zipEntryName.endsWith("/")) {
                zos.putNextEntry(new ZipEntry(zipEntryName + "/")); // Corrected line
                zos.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            if (children != null) {
                for (File childFile : children) {
                    zipFileRecursive(childFile, zipEntryName + "/" + childFile.getName(), zos);
                }
            }
            return;
        }

        try (FileInputStream fis = new FileInputStream(fileToZip)) {
            ZipEntry zipEntry = new ZipEntry(zipEntryName);
            zos.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
        }
    }

    private void unzipFile(File zipFile, File destDir) throws IOException {
        if (!destDir.exists()) destDir.mkdirs();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buffer = new byte[1024];
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    File parent = newFile.getParentFile();
                    if (!parent.exists()) parent.mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private void compressGzipFile(File inputFile) throws IOException {
        File outputFile = new File(inputFile.getParent(), inputFile.getName() + ".gz");
        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile);
             GZIPOutputStream gzipOS = new GZIPOutputStream(fos)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                gzipOS.write(buffer, 0, len);
            }
        }
    }

    private void decompressGzipFile(File gzipFile) throws IOException {
        String outputFileName = gzipFile.getName().substring(0, gzipFile.getName().length() - 3);
        File outputFile = new File(gzipFile.getParent(), outputFileName);
        try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(gzipFile));
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        }
    }

    public static void openTerminalAt(File folder) throws IOException {
    String os = System.getProperty("os.name").toLowerCase();
    String folderPath = folder.getAbsolutePath();

    if (os.contains("win")) {
        // Windows: cmd.exe starting in folder
        new ProcessBuilder("cmd.exe", "/c", "start", "cmd.exe", "/K", "cd /d " + folderPath).start();
    } else if (os.contains("mac")) {
        // macOS: open Terminal.app and cd to folder
        String[] cmd = {"/usr/bin/osascript", "-e",
            "tell application \"Terminal\" to do script \"cd '" + folderPath + "'\""};
        Runtime.getRuntime().exec(cmd);
    } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
        // Linux: try gnome-terminal or xterm
        String[] terminals = {
            "gnome-terminal", "konsole", "xfce4-terminal", "xterm"
        };
        boolean started = false;
        for (String term : terminals) {
            try {
                new ProcessBuilder(term, "--working-directory=" + folderPath).start();
                started = true;
                break;
            } catch (IOException ignored) {}
        }
        if (!started) {
            throw new IOException("No supported terminal emulator found.");
        }
    } else {
        throw new IOException("Unsupported OS: " + os);
    }
}

    public static List<File> findLargeFiles(File folder, long minSizeBytes) {
    List<File> result = new ArrayList<>();
    if (folder == null || !folder.isDirectory()) return result;

    File[] files = folder.listFiles();
    if (files == null) return result;

    for (File f : files) {
        if (f.isDirectory()) {
            result.addAll(findLargeFiles(f, minSizeBytes));
        } else if (f.length() >= minSizeBytes) {
            result.add(f);
        }
    }
    return result;
}

    public static List<List<File>> findDuplicatesInFolder(File folder) throws Exception {
    Map<String, List<File>> hashToFileList = new HashMap<>();
    findFilesByHash(folder, hashToFileList);

    List<List<File>> duplicates = new ArrayList<>();
    for (List<File> group : hashToFileList.values()) {
        if (group.size() > 1) duplicates.add(group);
    }
    return duplicates;
}

private static void findFilesByHash(File folder, Map<String, List<File>> map) throws Exception {
    if (folder == null || !folder.isDirectory()) return;

    File[] files = folder.listFiles();
    if (files == null) return;

    for (File f : files) {
        if (f.isDirectory()) {
            findFilesByHash(f, map);
        } else {
            String hash = calculateSHA256(f);
            map.computeIfAbsent(hash, k -> new ArrayList<>()).add(f);
        }
    }
}

    public static String calculateSHA256(File file) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    try (FileInputStream fis = new FileInputStream(file)) {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = fis.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
        }
    }
    byte[] hashBytes = digest.digest();

    StringBuilder sb = new StringBuilder();
    for (byte b : hashBytes) {
        sb.append(String.format("%02x", b));
    }
    return sb.toString();
}

private void compareFolders(File folder1, File folder2) {
    Map<String, Long> folder1Files = listFilesWithSizes(folder1, folder1.getAbsolutePath());
    Map<String, Long> folder2Files = listFilesWithSizes(folder2, folder2.getAbsolutePath());

    StringBuilder report = new StringBuilder();
    for (String path : folder1Files.keySet()) {
        if (!folder2Files.containsKey(path)) {
            report.append("Missing in folder 2: ").append(path).append("\n");
        } else if (!folder1Files.get(path).equals(folder2Files.get(path))) {
            report.append("Different size: ").append(path).append("\n");
        }
    }
    for (String path : folder2Files.keySet()) {
        if (!folder1Files.containsKey(path)) {
            report.append("Missing in folder 1: ").append(path).append("\n");
        }
    }
    JOptionPane.showMessageDialog(this, report.length() > 0 ? report.toString() : "Folders are identical", "Comparison Result", JOptionPane.INFORMATION_MESSAGE);
}

private Map<String, Long> listFilesWithSizes(File folder, String rootPath) {
    Map<String, Long> files = new HashMap<>();
    File[] items = folder.listFiles();
    if (items != null) {
        for (File f : items) {
            if (f.isFile()) {
                String relativePath = f.getAbsolutePath().substring(rootPath.length() + 1);
                files.put(relativePath, f.length());
            } else if (f.isDirectory()) {
                files.putAll(listFilesWithSizes(f, rootPath));
            }
        }
    }
    return files;
}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DiskUsageAnalyzer().setVisible(true));
    }
}