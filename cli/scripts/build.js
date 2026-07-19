const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const rootDir = path.resolve(__dirname, '..');
const distDir = path.join(rootDir, 'dist');
const buildDir = path.join(rootDir, 'build');
const pkgDir = path.join(buildDir, 'package');

console.log('--- Starting Memora CLI Build & Package Pipeline ---');

// 1. Clean build & dist
if (fs.existsSync(distDir)) {
  fs.rmSync(distDir, { recursive: true, force: true });
}
if (fs.existsSync(buildDir)) {
  fs.rmSync(buildDir, { recursive: true, force: true });
}
const buildInfo = path.join(rootDir, 'tsconfig.tsbuildinfo');
if (fs.existsSync(buildInfo)) {
  fs.rmSync(buildInfo, { force: true });
}

// 2. Compile TypeScript
console.log('Compiling TypeScript...');
try {
  execSync('npx tsc', { cwd: rootDir, stdio: 'inherit' });
} catch (err) {
  console.error('TypeScript compilation failed:', err.message);
  process.exit(1);
}

// Dynamically require compiled event publisher and release manager
const { commandEventPublisher } = require('../dist/events/commandEvents');
const { ReleaseManager } = require('../dist/utils/releaseManager');
const { VersionProvider } = require('../dist/utils/versionProvider');

// Subscribe to log events to terminal during build
commandEventPublisher.subscribe({
  onEvent(e) {
    console.log(`[EVENT] [${e.type}] ${JSON.stringify(e.payload)}`);
  }
});

const version = VersionProvider.getVersionString();
commandEventPublisher.publish({
  type: 'BuildStarted',
  timestamp: new Date(),
  payload: { version }
});

// 3. Create Package Directory
fs.mkdirSync(pkgDir, { recursive: true });

// 4. Copy Assets
console.log('Copying assets into package layout...');
fs.mkdirSync(path.join(pkgDir, 'dist'), { recursive: true });
fs.cpSync(distDir, path.join(pkgDir, 'dist'), { recursive: true });

// Copy package.json, README, LICENSE, and write CHANGELOG placeholder
const assets = ['package.json', 'README.md', 'LICENSE'];
for (const asset of assets) {
  const src = path.join(rootDir, asset);
  if (fs.existsSync(src)) {
    fs.copyFileSync(src, path.join(pkgDir, asset));
  }
}
fs.writeFileSync(path.join(pkgDir, 'CHANGELOG.md'), '# Changelog\n\n## 0.0.1-alpha.0\n- Initial packaging framework setup.\n');

// 5. Validate Release layout
console.log('Validating release layout...');
const validated = ReleaseManager.validateRelease(pkgDir);
if (!validated) {
  commandEventPublisher.publish({
    type: 'BuildFailed',
    timestamp: new Date(),
    payload: { version, error: 'Release layout validation failed' }
  });
  console.error('Release validation failed!');
  process.exit(1);
}

// 6. Pack into Tarball
console.log('Packing tarball...');
// Run npm pack in build/package/
const packOutput = execSync('npm pack', { cwd: pkgDir }).toString().trim();
const tarballName = packOutput.split('\n').pop().trim();
const tarballSrc = path.join(pkgDir, tarballName);
const tarballDest = path.join(buildDir, tarballName);
fs.renameSync(tarballSrc, tarballDest);

const stats = fs.statSync(tarballDest);
commandEventPublisher.publish({
  type: 'PackageCreated',
  timestamp: new Date(),
  payload: { tarballPath: tarballDest, sizeBytes: stats.size }
});

// 7. Generate metadata JSON
console.log('Generating release metadata...');
const metadata = ReleaseManager.generateMetadata(tarballDest);
fs.writeFileSync(path.join(buildDir, 'metadata.json'), JSON.stringify(metadata, null, 2));

// 8. Verify Package content
console.log('Verifying package content...');
const verified = ReleaseManager.verifyPackage(tarballDest, metadata.checksum);
if (!verified) {
  commandEventPublisher.publish({
    type: 'BuildFailed',
    timestamp: new Date(),
    payload: { version, error: 'Package verification failed' }
  });
  console.error('Package verification failed!');
  process.exit(1);
}

// 9. Checksums & Future Signing Placeholders
fs.writeFileSync(path.join(buildDir, 'checksums.sha256'), `${metadata.checksum}  ${tarballName}\n`);
fs.writeFileSync(path.join(buildDir, 'signature.sig.placeholder'), 'FUTURE_DIGITAL_SIGNATURE_DATA\n');

commandEventPublisher.publish({
  type: 'BuildCompleted',
  timestamp: new Date(),
  payload: { version, outputPath: tarballDest }
});

console.log(`Successfully completed build pipeline!`);
console.log(`Package Tarball: ${tarballDest}`);
console.log(`SHA-256:        ${metadata.checksum}`);
