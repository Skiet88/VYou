import { z } from 'zod';

/**
 * Zod 4 can compile validators with `new Function` for speed, and probes whether that's allowed.
 * Our Content-Security-Policy forbids eval (no 'unsafe-eval'), so the probe is reported as a CSP
 * violation even though Zod catches it. Jitless mode never attempts it. Must run before any
 * schema is parsed, so it's the first import in main.tsx.
 */
z.config({ jitless: true });
