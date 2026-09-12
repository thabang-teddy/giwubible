<?php

declare(strict_types=1);

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

/**
 * Web-only maintenance gate driven by MAINTENANCE_MODE in .env, so it can be
 * toggled by editing the persistent .env file on the server directly — no
 * SSH/artisan access required. Deliberately excluded from the `api` group:
 * routes/api.php is the Flutter client's contract and must stay reachable.
 */
class CheckEnvMaintenanceMode
{
    public function handle(Request $request, Closure $next): Response
    {
        if (! config('app.maintenance_mode')) {
            return $next($request);
        }

        return response()
            ->view('maintenance', [
                'message' => config('app.maintenance_message'),
            ], 503)
            ->header('Retry-After', 3600);
    }
}
