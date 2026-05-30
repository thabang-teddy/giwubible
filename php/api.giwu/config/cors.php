<?php

return [
    'paths' => ['api/*', 'downloads/*'],
    'allowed_methods' => ['GET'],
    'allowed_origins' => [env('FRONTEND_URL', 'http://localhost:5173')],
    'allowed_origins_patterns' => [],
    'allowed_headers' => ['*'],
    'exposed_headers' => ['Content-Length', 'Content-Disposition'],
    'max_age' => 0,
    'supports_credentials' => false,
];
