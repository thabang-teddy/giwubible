<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\KeyEnglish;
use Illuminate\Http\JsonResponse;

class BookController extends Controller
{
    public function index(): JsonResponse
    {
        
        return response()->json(['error' => 'Unknown bible version','code'  => 'INVALID_BIBLE'], 422);
        
        // $books = KeyEnglish::select('b', 'n', 't')
        //     ->orderBy('b')
        //     ->get();

        // return response()->json(['data' => $books]);
    }
}
