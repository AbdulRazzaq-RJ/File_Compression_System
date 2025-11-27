// ---------- Huffman Data Structures ----------

class HuffmanNode {
  constructor(char, freq, left = null, right = null) {
    this.char = char;
    this.freq = freq;
    this.left = left;
    this.right = right;
  }

  get isLeaf() {
    return !this.left && !this.right;
  }
}

class MinHeap {
  constructor() {
    this.data = [];
  }

  size() {
    return this.data.length;
  }

  push(node) {
    this.data.push(node);
    this._bubbleUp(this.data.length - 1);
  }

  pop() {
    if (this.data.length === 0) return null;
    if (this.data.length === 1) return this.data.pop();
    const top = this.data[0];
    this.data[0] = this.data.pop();
    this._bubbleDown(0);
    return top;
  }

  _bubbleUp(index) {
    while (index > 0) {
      const parent = Math.floor((index - 1) / 2);
      if (this.data[parent].freq <= this.data[index].freq) break;
      [this.data[parent], this.data[index]] = [this.data[index], this.data[parent]];
      index = parent;
    }
  }

  _bubbleDown(index) {
    const n = this.data.length;
    while (true) {
      let smallest = index;
      const left = 2 * index + 1;
      const right = 2 * index + 2;

      if (left < n && this.data[left].freq < this.data[smallest].freq) {
        smallest = left;
      }
      if (right < n && this.data[right].freq < this.data[smallest].freq) {
        smallest = right;
      }
      if (smallest === index) break;
      [this.data[smallest], this.data[index]] = [this.data[index], this.data[smallest]];
      index = smallest;
    }
  }
}

// ---------- Huffman Core Functions ----------

function buildFrequencyTable(text) {
  const freq = {};
  for (const ch of text) {
    freq[ch] = (freq[ch] || 0) + 1;
  }
  return freq;
}

function buildHuffmanTree(freqMap) {
  const heap = new MinHeap();
  for (const [ch, freq] of Object.entries(freqMap)) {
    heap.push(new HuffmanNode(ch, freq));
  }

  if (heap.size() === 0) return null;

  // Edge case: only one unique character
  if (heap.size() === 1) {
    const only = heap.pop();
    return new HuffmanNode(null, only.freq, only, null);
  }

  while (heap.size() > 1) {
    const left = heap.pop();
    const right = heap.pop();
    const parent = new HuffmanNode(null, left.freq + right.freq, left, right);
    heap.push(parent);
  }
  return heap.pop();
}

function generateCodes(node, prefix, codeMap) {
  if (!node) return;
  if (node.isLeaf && node.char !== null) {
    codeMap[node.char] = prefix || "0"; // in case of single-char input
    return;
  }
  generateCodes(node.left, prefix + "0", codeMap);
  generateCodes(node.right, prefix + "1", codeMap);
}

// Encode text to bytes and bit length
function encodeText(text, codeMap) {
  let bitString = "";
  for (const ch of text) {
    bitString += codeMap[ch];
  }
  const bitLength = bitString.length;
  const byteCount = Math.ceil(bitLength / 8);
  const dataBytes = new Uint8Array(byteCount);

  let bitIndex = 0;
  for (let i = 0; i < byteCount; i++) {
    let byte = 0;
    for (let j = 0; j < 8; j++) {
      byte <<= 1;
      if (bitIndex < bitLength && bitString[bitIndex] === "1") {
        byte |= 1;
      }
      bitIndex++;
    }
    dataBytes[i] = byte;
  }

  return { dataBytes, bitLength };
}

// Decode bytes using tree + bitLength
function decodeBytes(dataBytes, root, bitLength) {
  let result = "";
  let node = root;
  let bitsRead = 0;

  for (const byte of dataBytes) {
    for (let i = 7; i >= 0 && bitsRead < bitLength; i--) {
      const bit = (byte >> i) & 1;
      node = bit === 0 ? node.left : node.right;

      if (node.isLeaf) {
        result += node.char;
        node = root;
      }
      bitsRead++;
    }
  }
  return result;
}

// ---------- UI Wiring ----------

document.addEventListener("DOMContentLoaded", () => {
  const logArea = document.getElementById("logArea");

  const compressInput = document.getElementById("compressInput");
  const compressFileInfo = document.getElementById("compressFileInfo");
  const chooseCompressFileBtn = document.getElementById("chooseCompressFileBtn");
  const compressBtn = document.getElementById("compressBtn");
  const compressDownloadLink = document.getElementById("compressDownloadLink");

  const origSizeEl = document.getElementById("origSize");
  const compSizeEl = document.getElementById("compSize");
  const compRatioEl = document.getElementById("compRatio");
  const compTimeEl = document.getElementById("compTime");

  const decompressInput = document.getElementById("decompressInput");
  const decompressFileInfo = document.getElementById("decompressFileInfo");
  const chooseDecompressFileBtn = document.getElementById("chooseDecompressFileBtn");
  const decompressBtn = document.getElementById("decompressBtn");
  const decompressDownloadLink = document.getElementById("decompressDownloadLink");

  const decompCompSizeEl = document.getElementById("decompCompSize");
  const decompOrigSizeEl = document.getElementById("decompOrigSize");
  const decompTimeEl = document.getElementById("decompTime");

  let compressFile = null;
  let decompressFile = null;

  function log(message) {
    const time = new Date().toLocaleTimeString();
    logArea.textContent = `[${time}] ${message}\n` + logArea.textContent;
  }

  function formatBytes(bytes) {
    if (bytes === 0) return "0 B";
    const units = ["B", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    const value = bytes / Math.pow(1024, i);
    return `${value.toFixed(i === 0 ? 0 : 2)} ${units[i]}`;
  }

  function enableDownloadLink(linkEl, blob, filename, label) {
    const url = URL.createObjectURL(blob);
    linkEl.href = url;
    linkEl.download = filename;
    linkEl.textContent = label;
    linkEl.classList.remove("hidden");
  }

  // ---------- Compress Handlers ----------

  chooseCompressFileBtn.addEventListener("click", () => {
    compressInput.click();
  });

  compressInput.addEventListener("change", (e) => {
    const file = e.target.files[0];
    compressFile = file || null;
    compressDownloadLink.classList.add("hidden");

    if (!file) {
      compressFileInfo.textContent = "No file selected.";
      compressBtn.disabled = true;
      return;
    }
    compressFileInfo.textContent = `${file.name} (${formatBytes(file.size)})`;
    compressBtn.disabled = false;
    log(`Selected file for compression: ${file.name}`);
  });

  compressBtn.addEventListener("click", () => {
    if (!compressFile) return;
    compressBtn.disabled = true;
    log("Starting compression...");

    const reader = new FileReader();
    reader.onload = (event) => {
      const text = event.target.result;
      if (!text || text.length === 0) {
        log("The selected file is empty. Nothing to compress.");
        compressBtn.disabled = false;
        return;
      }

      const start = performance.now();
      const freqMap = buildFrequencyTable(text);
      const root = buildHuffmanTree(freqMap);

      if (!root) {
        log("No data to compress.");
        compressBtn.disabled = false;
        return;
      }

      const codeMap = {};
      generateCodes(root, "", codeMap);

      const { dataBytes, bitLength } = encodeText(text, codeMap);

      // Header holds: freq table + bitLength
      const header = {
        bitLength,
        freq: freqMap
      };

      const encoder = new TextEncoder();
      const headerBytes = encoder.encode(JSON.stringify(header));
      const headerLength = headerBytes.length;

      // Layout: [4 bytes headerLength][headerBytes][dataBytes]
      const totalLength = 4 + headerLength + dataBytes.length;
      const buffer = new ArrayBuffer(totalLength);
      const view = new DataView(buffer);
      view.setUint32(0, headerLength);
      const uint8 = new Uint8Array(buffer);
      uint8.set(headerBytes, 4);
      uint8.set(dataBytes, 4 + headerLength);

      const blob = new Blob([buffer], { type: "application/octet-stream" });
      const end = performance.now();

      // Stats
      const originalSize = compressFile.size;
      const compressedSize = blob.size;
      const ratio = ((compressedSize / originalSize) * 100).toFixed(2);
      const timeMs = end - start;

      origSizeEl.textContent = formatBytes(originalSize);
      compSizeEl.textContent = formatBytes(compressedSize);
      compRatioEl.textContent = `${ratio}%`;
      compTimeEl.textContent = `${timeMs.toFixed(2)} ms`;

      const baseName = compressFile.name.replace(/\.[^/.]+$/, "");
      const outName = `${baseName || "compressed"}.huff`;
      enableDownloadLink(blob, outName, `Download ${outName}`);

      log(`Compression finished. Ratio: ${ratio}%. Time: ${timeMs.toFixed(2)} ms`);
      compressBtn.disabled = false;
    };

    reader.onerror = () => {
      log("Error reading file for compression.");
      compressBtn.disabled = false;
    };

    reader.readAsText(compressFile);
  });

  // ---------- Decompress Handlers ----------

  chooseDecompressFileBtn.addEventListener("click", () => {
    decompressInput.click();
  });

  decompressInput.addEventListener("change", (e) => {
    const file = e.target.files[0];
    decompressFile = file || null;
    decompressDownloadLink.classList.add("hidden");

    if (!file) {
      decompressFileInfo.textContent = "No file selected.";
      decompressBtn.disabled = true;
      return;
    }
    decompressFileInfo.textContent = `${file.name} (${formatBytes(file.size)})`;
    decompressBtn.disabled = false;
    log(`Selected file for decompression: ${file.name}`);
  });

  decompressBtn.addEventListener("click", () => {
    if (!decompressFile) return;
    decompressBtn.disabled = true;
    log("Starting decompression...");

    const reader = new FileReader();
    reader.onload = (event) => {
      const buffer = event.target.result;
      try {
        const start = performance.now();
        const view = new DataView(buffer);
        const headerLength = view.getUint32(0);

        const uint8 = new Uint8Array(buffer);
        const headerBytes = uint8.slice(4, 4 + headerLength);
        const dataBytes = uint8.slice(4 + headerLength);

        const decoder = new TextDecoder();
        const headerJson = decoder.decode(headerBytes);
        const header = JSON.parse(headerJson);

        const freqMap = header.freq;
        const bitLength = header.bitLength;

        const root = buildHuffmanTree(freqMap);
        const text = decodeBytes(dataBytes, root, bitLength);

        const blob = new Blob([text], { type: "text/plain;charset=utf-8" });
        const end = performance.now();

        const compressedSize = decompressFile.size;
        const decompressedSize = blob.size;
        const timeMs = end - start;

        decompCompSizeEl.textContent = formatBytes(compressedSize);
        decompOrigSizeEl.textContent = formatBytes(decompressedSize);
        decompTimeEl.textContent = `${timeMs.toFixed(2)} ms`;

        const baseName = decompressFile.name.replace(/\.huff$/i, "") || "decompressed";
        const outName = `${baseName}_decompressed.txt`;
        enableDownloadLink(blob, outName, `Download ${outName}`);

        log(`Decompression finished. Time: ${timeMs.toFixed(2)} ms`);
      } catch (err) {
        console.error(err);
        log("Error during decompression. Make sure this file was created by this tool.");
      } finally {
        decompressBtn.disabled = false;
      }
    };

    reader.onerror = () => {
      log("Error reading file for decompression.");
      decompressBtn.disabled = false;
    };

    reader.readAsArrayBuffer(decompressFile);
  });
});
