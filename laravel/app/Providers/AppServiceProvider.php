<?php

namespace App\Providers;

use App\Models\User;
use App\Support\VerificationMail;
use Illuminate\Auth\Events\Registered;
use Illuminate\Auth\Middleware\Authenticate;
use Illuminate\Contracts\Foundation\MaintenanceMode;
use Illuminate\Foundation\FileBasedMaintenanceMode;
use Illuminate\Support\Facades\Event;
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

        // Sends the verification link on sign-up. This app has no
        // EventServiceProvider and event discovery is off, so the listener
        // Laravel would normally wire up for a MustVerifyEmail user has to be
        // registered by hand — otherwise `event(new Registered($user))` in the
        // web and API register actions would fire into nothing.
        //
        // Laravel's own SendEmailVerificationNotification would do, but going
        // through VerificationMail keeps every send under one per-recipient
        // cap and one mail-failure policy.
        Event::listen(Registered::class, function (Registered $event): void {
            if ($event->user instanceof User) {
                VerificationMail::sendTo($event->user);
            }
        });
    }
}
