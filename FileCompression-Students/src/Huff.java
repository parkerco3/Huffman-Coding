import java.util.*;
import java.io.*;

public class Huff implements ITreeMaker, IHuffEncoder, IHuffModel, IHuffHeader {
  
    private HuffTree finalTree;
    private int headerLen;
    private Map<Integer, String> table;
    private int uncompressedCount;
    private Map<Integer, Integer> countTable;

    /**
     * Initialize state from a tree, the tree is obtained
     * from the treeMaker.
     * @return the map of chars/encoding
     */

    public Map<Integer, String> makeTable() {
        this.table = null;
        Map<Integer, String> huffTable = new HashMap<Integer, String>();
        IHuffBaseNode currentNode = finalTree.root();
        helper(currentNode, huffTable, "");
        this.table = huffTable;
        return huffTable;
    }

    private void helper(IHuffBaseNode node, Map<Integer, String> map, String coding) {
        if (node.isLeaf()) {
            map.put(((HuffLeafNode) node).element(), coding); 
        } else {
            helper(((HuffInternalNode) node).left(), map, coding + "0");
            helper(((HuffInternalNode) node).right(), map, coding + "1");
        }
    }

    /**
     * Returns coding, e.g., "010111" for specified chunk/character. It
     * is an error to call this method before makeTable has been
     * called.
     * @param i is the chunk for which the coding is returned
     * @return the huff encoding for the specified chunk
     */
    public String getCode(int i) {
        return this.table.get(i);
    }

    /**
     * @return a map of all characters and their frequency
     */
    public Map<Integer, Integer> showCounts() {
        return countTable;
    }

    /**
     * Return the  Huffman/coding tree.
     * @return the Huffman tree
     */
    public HuffTree makeHuffTree(InputStream stream) throws IOException {
        countTable = new HashMap<>();
        CharCounter cc = new CharCounter();
        uncompressedCount = cc.countAll(stream) * BITS_PER_WORD;
        countTable = cc.getTable();
        PriorityQueue<HuffTree> treeQ = new PriorityQueue <>();
        for (int key: this.showCounts().keySet()) {
            HuffTree currentNode = new HuffTree(key, this.showCounts().get(key));
            treeQ.add(currentNode);
        }
        HuffTree eofNode = new HuffTree(PSEUDO_EOF, 1);
        treeQ.add(eofNode);
        while (!treeQ.isEmpty()) {
            HuffTree firstNode = treeQ.peek();
            treeQ.remove();
            HuffTree secondNode = treeQ.peek();
            treeQ.remove();
            HuffTree newNode = new HuffTree(firstNode.root(), 
                    secondNode.root(),firstNode.weight() + secondNode.weight());
            treeQ.add(newNode);
            if (treeQ.size() == 1) {
                treeQ.remove();
                finalTree = newNode;
            }
        }
        return finalTree;  
    }

    /**
     * The number of bits in the header using the implementation, including
     * the magic number presumably stored.
     * @return the number of bits in the header
     */
    @Override
    public int headerSize() {
        return this.headerLen;
    }

    /**
     * Write the header, including magic number and all bits needed to
     * reconstruct a tree, e.g., using <code>readHeader</code>.
     * @param out is where the header is written
     * @return the size of the header
     */
    @Override
    public int writeHeader(BitOutputStream out) {
        this.headerLen = 0;
        out.write(BITS_PER_INT, MAGIC_NUMBER);
        this.headerLen += BITS_PER_INT;
        writeHeaderHelper(this.finalTree.root(), out);
        return this.headerSize();
    }

    private void writeHeaderHelper(IHuffBaseNode node, BitOutputStream out) {
        if (node.isLeaf()) {
            out.write(1,1);
            this.headerLen++;
            out.write(BITS_PER_WORD + 1, ((HuffLeafNode) node).element());
            this.headerLen += BITS_PER_WORD + 1;
        } else {
            out.write(1,0);
            this.headerLen++;
            writeHeaderHelper(((HuffInternalNode) node).left(), out);
            writeHeaderHelper(((HuffInternalNode) node).right(), out);
        }
    }

    /**
     * Read the header and return an ITreeMaker object corresponding to
     * the information/header read.
     * @param in is source of bits for header
     * @return an ITreeMaker object representing the tree stored in the header
     * @throws IOException if the header is bad, e.g., wrong MAGIC_NUMBER, wrong
     * number of bits, I/O error occurs reading
     */
    @Override
    public HuffTree readHeader(BitInputStream in) throws IOException {
        HuffTree compressedTree = null;
        try {
            int magic = in.read(BITS_PER_INT);
            if (magic != MAGIC_NUMBER) {
                throw new IOException();
            }
            compressedTree = readerHelper(in);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return compressedTree;
    }

    private HuffTree readerHelper(BitInputStream in) throws IOException {

        int nodeType = in.read(1);
        if (nodeType == 1) {
            int leaf = in.read(BITS_PER_WORD + 1);
            HuffTree leafNode = new HuffTree(leaf, 0);
            return leafNode;


        } else {
            HuffTree leftChild = readerHelper(in);
            HuffTree rightChild = readerHelper(in);
            HuffTree currentNode = new HuffTree(leftChild.root(), rightChild.root(), 0);
            return currentNode;
        }
    }


    /**
     * Write a compressed version of the data read by the InputStream parameter,
     * -- if the stream is not the same as the stream last passed to initialize,
     * then compression won't be optimal, but will still work. If force is
     * false, compression only occurs if it saves space. If force is true
     * compression results even if no bits are saved.
     * 
     * @param inFile is the input stream to be compressed
     * @param outFile   specifies the OutputStream/file to be written with compressed data
     * @param force  indicates if compression forced
     * @return the size of the compressed file
     */
    @Override
    public int write(String inFile, String outFile, boolean force) {
        int size = 0;
        try {
            BitInputStream in = new BitInputStream(inFile);
            BitOutputStream tempCounter = new BitOutputStream("temp");
            makeHuffTree(in);
            makeTable();

            size += writeHeader(tempCounter);
            tempCounter.close();
            File file = new File("temp");
            file.delete();
            in.reset();
            while (true) {
                int currentChar = in.read(BITS_PER_WORD);
                if (currentChar == -1) {
                    break;
                } else {
                    size += table.get(currentChar).length();
                }

            }
            in.close();
            size += table.get(PSEUDO_EOF).length();
            if (force || size < uncompressedCount) {
                BitOutputStream out = new BitOutputStream(outFile);
                writeHeader(out);
                in.reset();
                while (true) {
                    int currentChar = in.read(BITS_PER_WORD);
                    if (currentChar == -1) {
                        break;
                    } else {
                        for (int i = 0; i < table.get(currentChar).length(); i++) {
                            char stringChar = table.get(currentChar).charAt(i);
                            if (stringChar == '0') {
                                out.write(1,0);
                            } else {
                                out.write(1,1);
                            }

                        }
                    }

                }

                for (int i = 0; i < table.get(PSEUDO_EOF).length(); i++) {
                    char stringChar = table.get(PSEUDO_EOF).charAt(i);
                    if (stringChar == '0') {
                        out.write(1,0);
                    } else {
                        out.write(1,1);
                    }
                }
                in.close();
                out.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return size;
    }


    /**
     * Uncompress a previously compressed file.
     * 
     * @param inFile  is the compressed file to be uncompressed
     * @param outFile is where the uncompressed bits will be written
     * @return the size of the uncompressed file
     */
    @Override
    public int uncompress(String inFile, String outFile) {
        int sizeCounter = 0;
        try {
            BitInputStream in = new BitInputStream(inFile);
            FileOutputStream out = new FileOutputStream(outFile);
            HuffTree newTree = readHeader(in);
            IHuffBaseNode root = newTree.root();
            int bits;
            while (true) {
                bits = in.read(1);
                if (bits == -1) {
                    out.close();
                    throw new IOException("unexpected end of input file");
                } else { 

                    if (bits == 0) {
                        root = ((HuffInternalNode) root).left();

                    } else {
                        root = ((HuffInternalNode) root).right(); 
                    }

                    if (root.isLeaf()) {
                        if (((HuffLeafNode) root).element() == PSEUDO_EOF) {
                            out.close();
                            break;
                        } else {
                            out.write(((HuffLeafNode) root).element());
                            sizeCounter += BITS_PER_WORD;
                            root = newTree.root();
                        }
                    }
                }
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sizeCounter;
    } 
}


