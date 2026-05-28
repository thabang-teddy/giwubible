<?php

use App\Http\Controllers\Api\BibleController;
use App\Http\Controllers\Api\BibleDownloadController;
use App\Http\Controllers\Api\BookController;
use App\Http\Controllers\Api\ChapterController;
use App\Http\Controllers\Api\VerseController;
use Illuminate\Support\Facades\Route;

Route::get('/bibles', [BibleController::class, 'index']);
Route::get('/bibles/{table}/download', [BibleDownloadController::class, 'download']);
Route::get('/books', [BookController::class, 'index']);
Route::get('/chapter', [ChapterController::class, 'index']);
Route::get('/verse', [VerseController::class, 'show']);
