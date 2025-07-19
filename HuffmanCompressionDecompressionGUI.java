import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.filechooser.FileNameExtensionFilter;

class Node {
    char data;
    int frequency;
    Node left, right;

    Node(char data, int frequency) {
        this.data = data;
        this.frequency = frequency;
        left = right = null;
    }

    Node(int frequency) {
        this.frequency = frequency;
        left = right = null;
    }
}

public class HuffmanCompressionDecompressionGUI extends JFrame {

    private static final int MAX_CHAR = 65536; // Unicode range
    private int[] frequencyTable = new int[MAX_CHAR]; // Store frequencies
    private String[] huffmanCodes = new String[MAX_CHAR];
    private Node[] heap = new Node[MAX_CHAR]; // Min-heap array
    private int heapSize = 0;
    private int charCount = 0; // Number of unique characters

    private JTextArea outputTextArea;

    public HuffmanCompressionDecompressionGUI() {
        // Set up the GUI window
        setTitle("Huffman Compression/Decompression Tool");
        setSize(600, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main Panel with Border Layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(240, 248, 255)); // Light background

        // Header Label
        JLabel titleLabel = new JLabel("Huffman Compression/Decompression");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(new Color(25, 25, 112)); // Midnight blue
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Buttons Panel (Center)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(new Color(240, 248, 255));

        JButton compressButton = createStyledButton("Compress File", new Color(70, 130, 180));
        JButton decompressButton = createStyledButton("Decompress File", new Color(60, 179, 113));

        buttonPanel.add(compressButton);
        buttonPanel.add(decompressButton);

        // Text Area Panel (Bottom)
        outputTextArea = new JTextArea(12, 40);
        outputTextArea.setEditable(false);
        outputTextArea.setFont(new Font("Courier New", Font.PLAIN, 13));
        outputTextArea.setForeground(new Color(25, 25, 25));
        outputTextArea.setBackground(new Color(245, 245, 245)); // Light gray
        outputTextArea.setBorder(BorderFactory.createLineBorder(new Color(70, 130, 180), 2));

        JScrollPane scrollPane = new JScrollPane(outputTextArea);
        mainPanel.add(scrollPane, BorderLayout.SOUTH);

        // Add Panels to Main Frame
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        add(mainPanel);

        // Button Event Handlers
        compressButton.addActionListener(e -> handleCompressButtonClick());
        decompressButton.addActionListener(e -> handleDecompressButtonClick());
    }

    // Method to create styled buttons
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setPreferredSize(new Dimension(200, 40));
        return button;
    }

    // Build frequency table from input text and track unique character count
    private void buildFrequencyTable(String filePath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath), 8192)) {
            int ch;
            while ((ch = br.read()) != -1) {
                if (frequencyTable[ch] == 0) {
                    charCount++; // Count unique characters
                }
                frequencyTable[ch]++;
            }
        }
    }

    private void insertHeap(Node node) {
        heap[heapSize] = node;
        int currentIndex = heapSize++;
        while (currentIndex > 0) {
            int parentIndex = (currentIndex - 1) / 2;
            if (heap[currentIndex].frequency >= heap[parentIndex].frequency) {
                break;
            }
            swap(currentIndex, parentIndex);
            currentIndex = parentIndex;
        }
    }

    private Node extractMin() {
        Node minNode = heap[0];
        heap[0] = heap[--heapSize];
        heapify(0);
        return minNode;
    }

    private void heapify(int index) {
        int smallest = index;
        int leftChild = 2 * index + 1;
        int rightChild = 2 * index + 2;

        if (leftChild < heapSize && heap[leftChild].frequency < heap[smallest].frequency) {
            smallest = leftChild;
        }

        if (rightChild < heapSize && heap[rightChild].frequency < heap[smallest].frequency) {
            smallest = rightChild;
        }

        if (smallest != index) {
            swap(smallest, index);
            heapify(smallest);
        }
    }

    private void swap(int i, int j) {
        Node temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
    }

    private Node buildHuffmanTree() {
        for (int i = 0; i < MAX_CHAR; i++) {
            if (frequencyTable[i] > 0) {
                insertHeap(new Node((char) i, frequencyTable[i]));
            }
        }

        while (heapSize > 1) {
            Node node1 = extractMin();
            Node node2 = extractMin();
            Node newNode = new Node(node1.frequency + node2.frequency);
            newNode.left = node1;
            newNode.right = node2;
            insertHeap(newNode);
        }
        return heap[0]; // Root of the Huffman Tree
    }

    private void generateHuffmanCodes(Node node, String code) {
        if (node.left == null && node.right == null) {
            huffmanCodes[node.data] = code;
            return;
        }
        generateHuffmanCodes(node.left, code + "0");
        generateHuffmanCodes(node.right, code + "1");
    }

    private void compressFile(String inputFilePath, String outputFilePath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath), 8192);
                DataOutputStream dos = new DataOutputStream(
                        new BufferedOutputStream(new FileOutputStream(outputFilePath), 8192))) {

            dos.writeInt(charCount);

            for (int i = 0; i < MAX_CHAR; i++) {
                if (frequencyTable[i] > 0) {
                    dos.writeChar(i);
                    dos.writeInt(frequencyTable[i]);
                }
            }

            int bitBuffer = 0;
            int bitCount = 0;
            int ch;

            while ((ch = br.read()) != -1) {
                String code = huffmanCodes[ch];
                for (char bit : code.toCharArray()) {
                    bitBuffer <<= 1;
                    if (bit == '1') {
                        bitBuffer |= 1;
                    }
                    bitCount++;
                    if (bitCount == 8) {
                        dos.write(bitBuffer);
                        bitBuffer = 0;
                        bitCount = 0;
                    }
                }
            }

            if (bitCount > 0) {
                bitBuffer <<= (8 - bitCount);
                dos.write(bitBuffer);
            }
        }
    }

    private long getFileSize(String filePath) {
        File file = new File(filePath);
        return file.length();
    }

    private void handleCompressButtonClick() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File inputFile = fileChooser.getSelectedFile();

            try {
                String inputFilePath = inputFile.getAbsolutePath();
                String outputFilePath = inputFile.getParent() + "/compressed.bin";

                // Step 1: Build frequency table
                buildFrequencyTable(inputFilePath);

                // Step 2: Build Huffman Tree
                Node root = buildHuffmanTree();
                if (root == null) {
                    outputTextArea.setText("The input file is empty. No compression needed.");
                    return; // Exit
                }

                // Step 3: Generate Huffman codes
                generateHuffmanCodes(root, "");

                // Step 4: Compress the file
                long startTime = System.nanoTime(); // Start timing compression
                compressFile(inputFilePath, outputFilePath);
                long endTime = System.nanoTime(); // End timing compression
                double compressionTimeInMillis = (endTime - startTime) / 1e6;

                // Time conversion into minutes, seconds, and milliseconds
                long totalTimeInMillis = (long) compressionTimeInMillis;
                long minutes = totalTimeInMillis / 60000;
                long seconds = (totalTimeInMillis % 60000) / 1000;
                long millis = totalTimeInMillis % 1000;

                // Step 5: Show file sizes and compression ratio
                long originalSize = getFileSize(inputFilePath);
                long compressedSize = getFileSize(outputFilePath);
                double compressionRatio = (double) (compressedSize) / originalSize * 100; // Percentage

                outputTextArea.setText(String.format("Original File Size: %d bytes\n", originalSize));
                outputTextArea.append(String.format("Compressed File Size: %d bytes\n", compressedSize));
                outputTextArea.append(String.format("Compression Ratio: %.2f%%\n", compressionRatio));
                outputTextArea.append(
                        String.format("Time Taken to Compress: %d min %d sec %d ms\n", minutes, seconds, millis));

            } catch (IOException ex) {
                outputTextArea.setText("Error: " + ex.getMessage());
            }
        }
    }

    private void handleDecompressButtonClick() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Compressed Files", "bin"));
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File inputFile = fileChooser.getSelectedFile();

            try {
                String compressedFilePath = inputFile.getAbsolutePath();
                String decompressedFilePath = inputFile.getParent() + "/decompressed.txt";

                // Step 1: Decompress the file
                long startTime = System.nanoTime(); // Start timing decompression
                decompressFile(compressedFilePath, decompressedFilePath);
                long endTime = System.nanoTime(); // End timing decompression
                double decompressionTimeInMillis = (endTime - startTime) / 1e6;

                // Time conversion into minutes, seconds, and milliseconds
                long totalTimeInMillis = (long) decompressionTimeInMillis;
                long minutes = totalTimeInMillis / 60000;
                long seconds = (totalTimeInMillis % 60000) / 1000;
                long millis = totalTimeInMillis % 1000;

                // Step 2: Show file sizes
                long compressedSize = getFileSize(compressedFilePath);
                long decompressedSize = getFileSize(decompressedFilePath);

                outputTextArea.setText(String.format("Compressed File Size: %d bytes\n", compressedSize));
                outputTextArea.append(String.format("Decompressed File Size: %d bytes\n", decompressedSize));
                outputTextArea.append(
                        String.format("Time Taken to Decompress: %d min %d sec %d ms\n", minutes, seconds, millis));

            } catch (IOException ex) {
                outputTextArea.setText("Error: " + ex.getMessage());
            }
        }
    }

    // Decompression logic (same as the provided code)
    private void decompressFile(String inputFilePath, String outputFilePath) throws IOException {
        // Decompression implementation (the existing code for decompression)
        // You can reuse the decompression logic as it is from your original code.
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(inputFilePath), 8192));
                BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {

            // Step 1: Read the number of unique characters
            int uniqueCharsCount = dis.readInt();

            // Step 2: Rebuild the frequency table and create the Huffman Tree
            int[] frequencyTable = new int[MAX_CHAR];
            for (int i = 0; i < uniqueCharsCount; i++) {
                char ch = dis.readChar();
                int freq = dis.readInt();
                frequencyTable[ch] = freq;
            }

            // Step 3: Build Huffman Tree from the frequency table
            buildHuffmanTreeFromFrequencyTable(frequencyTable);

            // Step 4: Start decoding the file
            Node root = heap[0]; // The root of the Huffman Tree
            Node currentNode = root;
            int bitBuffer = 0;
            int bitCount = 0;
            int ch;

            while ((ch = dis.read()) != -1) {
                bitBuffer = (bitBuffer << 8) | ch; // Add the next byte to the bitBuffer
                bitCount += 8;

                while (bitCount >= 1) {
                    int bit = (bitBuffer >> (bitCount - 1)) & 1;
                    currentNode = (bit == 0) ? currentNode.left : currentNode.right;
                    bitCount--;

                    // If leaf node is reached, output the character
                    if (currentNode.left == null && currentNode.right == null) {
                        writer.write(currentNode.data);
                        currentNode = root; // Reset to root for the next character
                    }
                }
            }

            writer.flush(); // Make sure everything is written to the output file
        }
    }

    // Helper method to rebuild the Huffman tree from the frequency table
    private void buildHuffmanTreeFromFrequencyTable(int[] frequencyTable) {
        // Clear the heap and rebuild it
        heapSize = 0;
        for (int i = 0; i < MAX_CHAR; i++) {
            if (frequencyTable[i] > 0) {
                insertHeap(new Node((char) i, frequencyTable[i]));
            }
        }

        // Build the tree by combining the nodes
        while (heapSize > 1) {
            Node node1 = extractMin();
            Node node2 = extractMin();
            Node newNode = new Node(node1.frequency + node2.frequency);
            newNode.left = node1;
            newNode.right = node2;
            insertHeap(newNode);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new HuffmanCompressionDecompressionGUI().setVisible(true);
        });
    }
}