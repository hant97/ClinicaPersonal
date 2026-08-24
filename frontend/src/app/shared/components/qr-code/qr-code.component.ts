import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';


/**
 * Lightweight, zero-dependency QR Code generator component.
 * Encodes text into a standard QR code matrix (Byte Mode, ISO/IEC 18004) and renders scalable SVG.
 */
@Component({
  selector: 'app-qr-code',
  standalone: true,
  imports: [],
  template: `
    <div class="qr-container inline-block" [style.width.px]="size" [style.height.px]="size">
      <svg
        [attr.viewBox]="'0 0 ' + matrixSize + ' ' + matrixSize"
        class="w-full h-full"
        shape-rendering="crispEdges"
        aria-label="Código QR"
      >
        <rect width="100%" height="100%" [attr.fill]="bgColor" />
        <path [attr.d]="svgPath" [attr.fill]="fgColor" />
      </svg>
    </div>
  `,
  styles: [`
    :host {
      display: inline-block;
    }
  `]
})
export class QrCodeComponent implements OnChanges {
  @Input() value = '';
  @Input() size = 120;
  @Input() fgColor = '#0f172a';
  @Input() bgColor = '#ffffff';

  matrixSize = 25;
  svgPath = '';

  ngOnChanges(_changes: SimpleChanges): void {
    this.generateQr();
  }

  private generateQr(): void {
    if (!this.value) {
      this.svgPath = '';
      return;
    }

    try {
      const matrix = QRCodeMatrix.create(this.value);
      this.matrixSize = matrix.length;
      let path = '';
      for (let r = 0; r < matrix.length; r++) {
        for (let c = 0; c < matrix[r].length; c++) {
          if (matrix[r][c]) {
            path += `M${c},${r}h1v1h-1z `;
          }
        }
      }
      this.svgPath = path.trim();
    } catch {
      this.svgPath = '';
    }
  }
}

/**
 * Standard QR Code Generator (Versions 1 - 4, Byte Mode, EC Level L / M)
 */
class QRCodeMatrix {
  static create(text: string): boolean[][] {
    const dataBytes = new TextEncoder().encode(text);
    // Select minimum QR version that fits dataBytes (Level M: V1<=14, V2<=26, V3<=42, V4<=62, V5<=84)
    let version = 1;
    if (dataBytes.length > 14) version = 2;
    if (dataBytes.length > 26) version = 3;
    if (dataBytes.length > 42) version = 4;
    if (dataBytes.length > 62) version = 5;
    if (dataBytes.length > 84) version = 6;

    const size = version * 4 + 17;
    const matrix: (boolean | null)[][] = Array.from({ length: size }, () => Array(size).fill(null));

    // 1. Finder patterns
    this.addFinderPattern(matrix, 0, 0);
    this.addFinderPattern(matrix, size - 7, 0);
    this.addFinderPattern(matrix, 0, size - 7);

    // 2. Timing patterns
    for (let i = 8; i < size - 8; i++) {
      matrix[6][i] = i % 2 === 0;
      matrix[i][6] = i % 2 === 0;
    }

    // 3. Alignment patterns (for version >= 2)
    if (version >= 2) {
      const pos = version === 2 ? [6, 18] : version === 3 ? [6, 22] : version === 4 ? [6, 26] : version === 5 ? [6, 30] : [6, 34];
      for (const r of pos) {
        for (const c of pos) {
          if (matrix[r][c] === null) {
            this.addAlignmentPattern(matrix, r, c);
          }
        }
      }
    }

    // 4. Dark module & reserved format info areas
    matrix[size - 8][8] = true;
    this.reserveFormatInfo(matrix, size);

    // 5. Data encoding & placement
    const bitBuffer = this.encodeData(dataBytes, version);
    this.placeDataBits(matrix, bitBuffer, size);

    // 6. Mask pattern (Mask 0: (r + c) % 2 == 0)
    for (let r = 0; r < size; r++) {
      for (let c = 0; c < size; c++) {
        if (!this.isFunctionPattern(r, c, size, version)) {
          if ((r + c) % 2 === 0) {
            matrix[r][c] = !matrix[r][c];
          }
        }
      }
    }

    // 7. Format info bits (Mask 0, Level M -> 101010000010010)
    const formatBits = [true, false, true, false, true, false, false, false, false, false, true, false, false, true, false];
    this.applyFormatBits(matrix, formatBits, size);

    return matrix.map(row => row.map(cell => !!cell));
  }

  private static addFinderPattern(matrix: (boolean | null)[][], row: number, col: number): void {
    for (let r = -1; r <= 7; r++) {
      for (let c = -1; c <= 7; c++) {
        const nr = row + r;
        const nc = col + c;
        if (nr >= 0 && nr < matrix.length && nc >= 0 && nc < matrix.length) {
          if (r >= 0 && r <= 6 && c >= 0 && c <= 6) {
            matrix[nr][nc] = (r === 0 || r === 6 || c === 0 || c === 6 || (r >= 2 && r <= 4 && c >= 2 && c <= 4));
          } else {
            matrix[nr][nc] = false;
          }
        }
      }
    }
  }

  private static addAlignmentPattern(matrix: (boolean | null)[][], row: number, col: number): void {
    for (let r = -2; r <= 2; r++) {
      for (let c = -2; c <= 2; c++) {
        matrix[row + r][col + c] = (Math.abs(r) === 2 || Math.abs(c) === 2 || (r === 0 && c === 0));
      }
    }
  }

  private static reserveFormatInfo(matrix: (boolean | null)[][], size: number): void {
    for (let i = 0; i < 9; i++) {
      if (matrix[8][i] === null) matrix[8][i] = false;
      if (matrix[i][8] === null) matrix[i][8] = false;
    }
    for (let i = 0; i < 8; i++) {
      if (matrix[8][size - 1 - i] === null) matrix[8][size - 1 - i] = false;
      if (matrix[size - 1 - i][8] === null) matrix[size - 1 - i][8] = false;
    }
  }

  private static applyFormatBits(matrix: (boolean | null)[][], bits: boolean[], size: number): void {
    const coords1 = [
      [8, 0], [8, 1], [8, 2], [8, 3], [8, 4], [8, 5], [8, 7], [8, 8],
      [7, 8], [5, 8], [4, 8], [3, 8], [2, 8], [1, 8], [0, 8]
    ];
    for (let i = 0; i < 15; i++) {
      matrix[coords1[i][0]][coords1[i][1]] = bits[i];
    }
    const coords2 = [
      [size - 1, 8], [size - 2, 8], [size - 3, 8], [size - 4, 8], [size - 5, 8], [size - 6, 8], [size - 7, 8],
      [8, size - 8], [8, size - 7], [8, size - 6], [8, size - 5], [8, size - 4], [8, size - 3], [8, size - 2], [8, size - 1]
    ];
    for (let i = 0; i < 15; i++) {
      matrix[coords2[i][0]][coords2[i][1]] = bits[i];
    }
  }

  private static isFunctionPattern(r: number, c: number, size: number, version: number): boolean {
    if (r <= 8 && (c <= 8 || c >= size - 8)) return true;
    if (r >= size - 8 && c <= 8) return true;
    if (r === 6 || c === 6) return true;
    if (version >= 2) {
      const pos = version === 2 ? [6, 18] : version === 3 ? [6, 22] : version === 4 ? [6, 26] : version === 5 ? [6, 30] : [6, 34];
      for (const pr of pos) {
        for (const pc of pos) {
          if (!(pr <= 8 && pc <= 8) && !(pr <= 8 && pc >= size - 8) && !(pr >= size - 8 && pc <= 8)) {
            if (Math.abs(r - pr) <= 2 && Math.abs(c - pc) <= 2) return true;
          }
        }
      }
    }
    return false;
  }

  private static encodeData(data: Uint8Array, version: number): boolean[] {
    const bits: boolean[] = [];
    const pushBits = (val: number, len: number) => {
      for (let i = len - 1; i >= 0; i--) bits.push(((val >> i) & 1) === 1);
    };

    // Mode Byte (0100)
    pushBits(4, 4);
    // Character count indicator (8 bits for V1-V9)
    pushBits(data.length, 8);
    // Data
    for (let i = 0; i < data.length; i++) {
      pushBits(data[i], 8);
    }
    // Terminator
    pushBits(0, 4);
    // Byte align
    while (bits.length % 8 !== 0) bits.push(false);

    // Total data capacity in bytes for level M
    const capacities = [0, 16, 28, 44, 64, 86, 108];
    const targetCapacity = capacities[version] || 16;
    const padBytes = [0xEC, 0x11];
    let padIdx = 0;
    while (bits.length < targetCapacity * 8) {
      pushBits(padBytes[padIdx % 2], 8);
      padIdx++;
    }

    // Convert bits to byte array
    const dataCodewords: number[] = [];
    for (let i = 0; i < bits.length; i += 8) {
      let b = 0;
      for (let j = 0; j < 8; j++) b = (b << 1) | (bits[i + j] ? 1 : 0);
      dataCodewords.push(b);
    }

    // Calculate Reed-Solomon EC bytes
    const ecCounts = [0, 10, 16, 26, 36, 48, 64];
    const ecCount = ecCounts[version] || 10;
    const ecCodewords = this.calculateReedSolomon(dataCodewords, ecCount);

    const allCodewords = [...dataCodewords, ...ecCodewords];
    const finalBits: boolean[] = [];
    for (const byte of allCodewords) {
      for (let i = 7; i >= 0; i--) finalBits.push(((byte >> i) & 1) === 1);
    }
    return finalBits;
  }

  private static calculateReedSolomon(data: number[], ecCount: number): number[] {
    const gfExp = new Uint8Array(512);
    const gfLog = new Uint8Array(256);
    let x = 1;
    for (let i = 0; i < 255; i++) {
      gfExp[i] = x;
      gfExp[i + 255] = x;
      gfLog[x] = i;
      x = (x << 1) ^ (x >= 128 ? 0x11D : 0);
    }

    const gfMul = (a: number, b: number): number => {
      if (a === 0 || b === 0) return 0;
      return gfExp[gfLog[a] + gfLog[b]];
    };

    // Generator polynomial
    let gen = [1];
    for (let i = 0; i < ecCount; i++) {
      const nextGen = new Array(gen.length + 1).fill(0);
      for (let j = 0; j < gen.length; j++) {
        nextGen[j] ^= gfMul(gen[j], gfExp[i]);
        nextGen[j + 1] ^= gen[j];
      }
      gen = nextGen;
    }

    const res = new Array(ecCount).fill(0);
    for (let i = 0; i < data.length; i++) {
      const factor = data[i] ^ res[0];
      res.shift();
      res.push(0);
      for (let j = 0; j < ecCount; j++) {
        res[j] ^= gfMul(gen[j], factor);
      }
    }
    return res;
  }

  private static placeDataBits(matrix: (boolean | null)[][], bits: boolean[], size: number): void {
    let bitIdx = 0;
    let up = true;
    for (let right = size - 1; right > 0; right -= 2) {
      if (right === 6) right--; // Skip vertical timing column
      const cols = [right, right - 1];
      const rows = up ? Array.from({ length: size }, (_, i) => size - 1 - i) : Array.from({ length: size }, (_, i) => i);
      for (const r of rows) {
        for (const c of cols) {
          if (matrix[r][c] === null) {
            matrix[r][c] = bitIdx < bits.length ? bits[bitIdx++] : false;
          }
        }
      }
      up = !up;
    }
  }
}
