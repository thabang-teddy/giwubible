<?php

namespace App\Providers;

use Illuminate\Cache\RateLimiting\Limit;
use Illuminate\Foundation\Support\Providers\RouteServiceProvider as ServiceProvider;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\RateLimiter;
use Illuminate\Support\Facades\Route;

class RouteServiceProvider extends ServiceProvider
{
    /**
     * The path to your application's "home" route.
     */
    public const HOME = '/';

    /**
     * Define your route model bindings, pattern filters, and other route configuration.
     */
    public function boot(): void
    {
        RateLimiter::for('api', function (Request $request) {
            return Limit::perMinute(60)->by($request->user()?->id ?: $request->ip());
        });

        // Each of the three email-verification routes gets its own budget.
        // An unnamed `throttle:6,1` keys on the caller alone, with no route in
        // the key, so the resend endpoint and the link itself would share one
        // counter — and a reader who pressed resend a few times would be
        // locked out of the link they were waiting for.
        RateLimiter::for('verification-send', function (Request $request) {
            return Limit::perMinute(6)->by($request->user()?->id ?: $request->ip());
        });

        RateLimiter::for('verification-verify', function (Request $request) {
            return Limit::perMinute(6)->by($request->user()?->id ?: $request->ip());
        });

        // Changing the address on a profile sends a verification link, which
        // makes an unthrottled profile write a way to mail a stranger in a
        // loop. VerificationMail caps it per recipient as well.
        RateLimiter::for('profile-update', function (Request $request) {
            return Limit::perMinute(6)->by($request->user()?->id ?: $request->ip());
        });

        // Registering now sends mail too, so the API route gets the web
        // route's limit rather than the much looser global API one.
        RateLimiter::for('register', function (Request $request) {
            return Limit::perMinute(10)->by($request->ip());
        });

        $this->routes(function () {
            Route::middleware('api')
                ->prefix('api')
                ->group(base_path('routes/api.php'));

            Route::middleware('web')
                ->group(base_path('routes/web.php'));
        });
    }
}
