<?php

use App\Http\Controllers\Web\AuthController;
use App\Http\Controllers\Web\BookmarkController;
use App\Http\Controllers\Web\DownloadController;
use App\Http\Controllers\Web\EmailVerificationController;
use App\Http\Controllers\Web\HomeController;
use App\Http\Controllers\Web\ProfileController;
use App\Http\Controllers\Web\ReadController;
use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| Public pages (Inertia)
|--------------------------------------------------------------------------
*/

Route::get('/', HomeController::class)->name('home');
Route::get('/read', ReadController::class)->name('read');
Route::get('/read/{book}/{chapter}', [ReadController::class, 'chapter'])
    ->whereNumber('chapter')
    ->name('read.chapter');
Route::get('/download', DownloadController::class)->name('download');
Route::get('/download/{platform}', [DownloadController::class, 'asset'])->name('download.asset');

/*
|--------------------------------------------------------------------------
| Auth (session / cookie — the web counterpart to the Sanctum token API)
|--------------------------------------------------------------------------
*/

Route::middleware('guest')->group(function () {
    Route::get('/login', [AuthController::class, 'showLogin'])->name('login');
    Route::post('/login', [AuthController::class, 'login'])->middleware('throttle:10,1');
    Route::post('/register', [AuthController::class, 'register'])->middleware('throttle:register')->name('register');
});

Route::post('/logout', [AuthController::class, 'logout'])->middleware('auth')->name('logout');

/*
|--------------------------------------------------------------------------
| Email verification
|--------------------------------------------------------------------------
| Signed in but unverified: these three are the only account routes such a
| reader can reach, along with the profile, which stays open on purpose so a
| mistyped address can still be corrected.
*/

Route::middleware('auth')->group(function () {
    Route::get('/email/verify', [EmailVerificationController::class, 'notice'])
        ->name('verification.notice');

    Route::get('/email/verify/{id}/{hash}', [EmailVerificationController::class, 'verify'])
        ->middleware(['signed', 'throttle:verification-verify'])
        ->name('verification.verify');

    Route::post('/email/verification-notification', [EmailVerificationController::class, 'resend'])
        ->middleware('throttle:verification-send')
        ->name('verification.send');
});

/*
|--------------------------------------------------------------------------
| Authenticated pages
|--------------------------------------------------------------------------
*/

Route::middleware('auth')->group(function () {
    Route::middleware('verified')->group(function () {
        Route::get('/bookmarks', [BookmarkController::class, 'index'])->name('bookmarks.index');
        Route::post('/bookmarks', [BookmarkController::class, 'store'])->name('bookmarks.store');
        Route::delete('/bookmarks/{id}', [BookmarkController::class, 'destroy'])->name('bookmarks.destroy');
    });

    Route::get('/profile', [ProfileController::class, 'edit'])->name('profile.edit');
    // Sends a verification link when the address changes.
    Route::put('/profile', [ProfileController::class, 'update'])
        ->middleware('throttle:profile-update')
        ->name('profile.update');
});
