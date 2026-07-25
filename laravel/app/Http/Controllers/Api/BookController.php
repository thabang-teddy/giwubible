<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Repositories\BibleRepository;
use Illuminate\Http\JsonResponse;

class BookController extends Controller
{
    public function index(BibleRepository $bibles): JsonResponse
    {
        return response()->json(['data' => $bibles->books()]);
    }
}
