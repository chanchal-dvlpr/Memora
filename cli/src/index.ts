#!/usr/bin/env node

import { run } from './cli';

run(process.argv)
  .then((exitCode) => {
    process.exit(exitCode);
  })
  .catch((err) => {
    process.stderr.write(`Fatal execution error: ${err.message}\n`);
    process.exit(1);
  });
