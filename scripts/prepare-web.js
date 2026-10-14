// Copies the offline photo-reading engine (Tesseract) into www/vendor.
const fs = require('fs');
const path = require('path');

function pkgDir(name) {
  return path.dirname(require.resolve(name + '/package.json'));
}
function copy(from, to) {
  fs.mkdirSync(path.dirname(to), { recursive: true });
  fs.copyFileSync(from, to);
  console.log('copied', path.relative('.', to));
}

const tess = pkgDir('tesseract.js');
copy(path.join(tess, 'dist', 'tesseract.min.js'), 'www/vendor/tesseract/tesseract.min.js');
copy(path.join(tess, 'dist', 'worker.min.js'), 'www/vendor/tesseract/worker.min.js');

const core = pkgDir('tesseract.js-core');
['tesseract-core-lstm.wasm.js', 'tesseract-core-simd-lstm.wasm.js', 'tesseract-core-relaxedsimd-lstm.wasm.js'].forEach((f) =>
  copy(path.join(core, f), path.join('www/vendor/tesseract-core', f))
);

['eng', 'ara'].forEach((l) =>
  copy(path.join(pkgDir('@tesseract.js-data/' + l), '4.0.0_best_int', l + '.traineddata.gz'), path.join('www/vendor/tessdata', l + '.traineddata.gz'))
);
