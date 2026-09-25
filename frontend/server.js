/**
 * MediLink 2.0 - Standalone Frontend Development Server
 * 
 * Zero-dependency Node.js HTTP server.
 * Serves static assets, handles SPA fallback, and maps MIME types.
 */
const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 3000;
const ROOT_DIR = path.resolve(__dirname, '../src/main/resources/static');

const MIME_TYPES = {
    '.html': 'text/html; charset=UTF-8',
    '.js': 'application/javascript; charset=UTF-8',
    '.css': 'text/css; charset=UTF-8',
    '.json': 'application/json; charset=UTF-8',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.jpeg': 'image/jpeg',
    '.gif': 'image/gif',
    '.svg': 'image/svg+xml',
    '.ico': 'image/x-icon',
    '.webp': 'image/webp',
    '.woff': 'font/woff',
    '.woff2': 'font/woff2',
    '.ttf': 'font/ttf',
    '.wav': 'audio/wav',
    '.mp3': 'audio/mpeg'
};

const server = http.createServer((req, res) => {
    // Add CORS headers
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', '*');

    if (req.method === 'OPTIONS') {
        res.writeHead(204);
        res.end();
        return;
    }

    // Reverse Proxy for Spring Boot Backend API (port 8080)
    if (req.url.startsWith('/api/') || req.url === '/api') {
        const proxyOptions = {
            hostname: 'localhost',
            port: 8080,
            path: req.url,
            method: req.method,
            headers: {
                ...req.headers,
                host: 'localhost:8080'
            }
        };

        const proxyReq = http.request(proxyOptions, (proxyRes) => {
            res.writeHead(proxyRes.statusCode, proxyRes.headers);
            proxyRes.pipe(res, { end: true });
        });

        proxyReq.on('error', (err) => {
            if (!res.headersSent) {
                res.writeHead(502, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({
                    status: 'ERROR',
                    message: 'Backend REST API (http://localhost:8080) is currently offline or unreachable.',
                    error: err.message
                }));
            }
        });

        req.pipe(proxyReq, { end: true });
        return;
    }

    // Sanitize URL and decode path
    let reqUrl = decodeURI(req.url.split('?')[0]);
    if (reqUrl === '/') reqUrl = '/index.html';
    if (reqUrl === '/forgot-password' || reqUrl === '/forgot-password/') reqUrl = '/forgot-password.html';

    let filePath = path.join(ROOT_DIR, reqUrl);

    // Prevent directory traversal
    if (!filePath.startsWith(ROOT_DIR)) {
        res.writeHead(403, { 'Content-Type': 'text/plain' });
        res.end('403 Forbidden');
        return;
    }

    fs.stat(filePath, (err, stats) => {
        if (err || !stats.isFile()) {
            // SPA fallback: return index.html for non-asset routes
            if (!path.extname(reqUrl)) {
                filePath = path.join(ROOT_DIR, 'index.html');
            } else {
                res.writeHead(404, { 'Content-Type': 'text/plain' });
                res.end(`404 Not Found: ${reqUrl}`);
                return;
            }
        }

        const ext = path.extname(filePath).toLowerCase();
        const contentType = MIME_TYPES[ext] || 'application/octet-stream';

        fs.readFile(filePath, (readErr, content) => {
            if (readErr) {
                res.writeHead(500, { 'Content-Type': 'text/plain' });
                res.end(`500 Internal Server Error: ${readErr.message}`);
                return;
            }

            res.writeHead(200, {
                'Content-Type': contentType,
                'Cache-Control': 'no-cache, no-store, must-revalidate'
            });
            res.end(content);
        });
    });
});

server.on('error', (err) => {
    if (err.code === 'EADDRINUSE') {
        console.log('\x1b[33m%s\x1b[0m', `[MediLink] Port ${PORT} is already running! Access at http://localhost:${PORT}`);
        process.exit(0);
    } else {
        console.error('[MediLink] Server error:', err);
    }
});

server.listen(PORT, () => {
    console.log('\x1b[36m%s\x1b[0m', '===================================================');
    console.log('\x1b[32m%s\x1b[0m', '  MediLink 2.0 - Standalone Frontend Client');
    console.log('\x1b[36m%s\x1b[0m', '===================================================');
    console.log('\x1b[33m%s\x1b[0m', `  Local URL:   http://localhost:${PORT}`);
    console.log('\x1b[90m%s\x1b[0m', `  Target API:  http://localhost:8080`);
    console.log('\x1b[90m%s\x1b[0m', `  Directory:   ${ROOT_DIR}`);
    console.log('\x1b[36m%s\x1b[0m', '---------------------------------------------------');
    console.log('\x1b[37m%s\x1b[0m', '  Ready to serve! Press Ctrl+C to stop.');
    console.log('');
});
