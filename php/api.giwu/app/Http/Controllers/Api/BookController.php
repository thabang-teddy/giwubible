<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\KeyEnglish;
use Illuminate\Http\JsonResponse;

class BookController extends Controller
{
    public function index(): JsonResponse
    {
        $books = KeyEnglish::select('b', 'n', 't')
            ->orderBy('b')
            ->get();

        return response()->json(['data' => $books]);
    }
}
