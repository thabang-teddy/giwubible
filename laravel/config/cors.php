<?php

return [
    'paths' => ['api/*', 'downloads/*'],
    'allowed_methods' => ['GET', 'POST', 'DELETE'],
    'allowed_origins' => [env('FRONTEND_URL', 'http://localhost:5173')],
    'allowed_origins_patterns' => [],
    'allowed_headers' => ['Content-Type', 'Authorization', 'Accept'],
    'exposed_headers' => ['Content-Length', 'Content-Disposition'],
    'max_age' => 3600,
    'supports_credentials' => false,
];
