<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\BibleVersionKey;
use Illuminate\Http\JsonResponse;

class BibleController extends Controller
{
    public function index(): JsonResponse
    {
        $bibles = BibleVersionKey::select('id', 'table', 'abbreviation', 'version', 'info_url')
            ->orderBy('abbreviation')
            ->get();

        return response()->json(['data' => $bibles]);
    }
}
