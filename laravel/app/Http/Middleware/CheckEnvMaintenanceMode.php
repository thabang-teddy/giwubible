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
 *
 * Reads .env's raw bytes instead of config()/env(). deploy/cpanel-deploy.sh
 * runs `artisan config:cache` on every release, which both freezes config()
 * at its deploy-time value AND stops Laravel from loading .env at all on
 * later requests (LoadEnvironmentVariables skips it once config is cached).
 * A flag read through config() or env() would therefore never change again
 * without another deploy — defeating the entire point of an env toggle.
 */
class CheckEnvMaintenanceMode
{
    public function handle(Request $request, Closure $next): Response
    {
        [$enabled, $message] = $this->readFlags();

        if (! $enabled) {
            return $next($request);
        }

        return response()
            ->view('maintenance', ['message' => $message], 503)
            ->header('Retry-After', 3600);
    }

    /**
     * @return array{0: bool, 1: ?string}
     */
    private function readFlags(): array
    {
        $path = base_path('.env');

        if (! is_file($path) || ! is_readable($path)) {
            return [false, null];
        }

        $enabled = false;
        $message = null;

        foreach (file($path, FILE_IGNORE_NEW_LINES) ?: [] as $line) {
            if (preg_match('/^\s*MAINTENANCE_MODE\s*=\s*(.*)$/i', $line, $matches) === 1) {
                $enabled = filter_var($this->unquote($matches[1]), FILTER_VALIDATE_BOOLEAN);
            } elseif (preg_match('/^\s*MAINTENANCE_MESSAGE\s*=\s*(.*)$/i', $line, $matches) === 1) {
                $message = $this->unquote($matches[1]) ?: null;
            }
        }

        return [$enabled, $message];
    }

    private function unquote(string $value): string
    {
        $value = trim($value);

        if (strlen($value) >= 2 && $value[0] === $value[-1] && in_array($value[0], ['"', "'"], true)) {
            $value = substr($value, 1, -1);
        }

        return $value;
    }
}
