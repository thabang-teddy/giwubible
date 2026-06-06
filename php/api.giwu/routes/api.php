<?php

use App\Http\Controllers\Api\AppDownloadController;
use App\Http\Controllers\Api\AuthController;
use App\Http\Controllers\Api\BibleController;
use App\Http\Controllers\Api\BibleDownloadController;
use App\Http\Controllers\Api\BookController;
use App\Http\Controllers\Api\BookmarkController;
use App\Http\Controllers\Api\ChapterController;
use App\Http\Controllers\Api\VerseController;
use Illuminate\Support\Facades\Route;

// Bible data (public)
Route::get('/bibles', [BibleController::class, 'index']);
Route::get('/bibles/{table}/download', [BibleDownloadController::class, 'download']);
Route::get('/books', [BookController::class, 'index']);
Route::get('/chapter', [ChapterController::class, 'index']);
Route::get('/verse', [VerseController::class, 'show']);
Route::get('/downloads/{filename}', [AppDownloadController::class, 'serve']);

// Auth
Route::post('/auth/register', [AuthController::class, 'register']);
Route::post('/auth/login', [AuthController::class, 'login']);

Route::middleware('auth:sanctum')->group(function () {
    Route::post('/auth/logout', [AuthController::class, 'logout']);
    Route::get('/auth/me', [AuthController::class, 'me']);

    Route::get('/bookmarks', [BookmarkController::class, 'index']);
    Route::post('/bookmarks', [BookmarkController::class, 'store']);
    Route::delete('/bookmarks/{id}', [BookmarkController::class, 'destroy']);
});
