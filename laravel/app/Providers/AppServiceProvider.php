<?php

namespace App\Providers;

use Illuminate\Auth\Middleware\Authenticate;
use Illuminate\Contracts\Foundation\MaintenanceMode;
use Illuminate\Foundation\FileBasedMaintenanceMode;
use Illuminate\Support\ServiceProvider;

class AppServiceProvider extends ServiceProvider
{
    /**
     * Register any application services.
     */
    public function register(): void
    {
        $this->app->bind(MaintenanceMode::class, FileBasedMaintenanceMode::class);
    }

    /**
     * Bootstrap any application services.
     */
    public function boot(): void
    {
        // Where an unauthenticated visitor is sent. Laravel 13 removed the
        // implicit `?? route('login')` fallback that Foundation's exception
        // handler used to apply: with no callback registered, a non-JSON
        // request to a route behind `auth` now gets a bodyless 401 instead of
        // the login page. That would hit /bookmarks and /profile.
        //
        // JSON requests are unaffected either way — shouldReturnJson() returns
        // the {"message":"Unauthenticated."} 401 before this is consulted, so
        // the Flutter client's contract does not move.
        Authenticate::redirectUsing(fn () => route('login'));
    }
}
