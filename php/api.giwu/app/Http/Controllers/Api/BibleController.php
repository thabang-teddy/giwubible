<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\BibleVersionKey;
use Illuminate\Http\JsonResponse;

class BibleController extends Controller
{
    public function index(): JsonResponse
    {
        
        return response()->json(['error' => 'No bible versions','code'  => 'INVALID_BIBLE'], 422);
        
        // $bibles = BibleVersionKey::select('id', 'table', 'abbreviation', 'version', 'info_url')
        //     ->orderBy('abbreviation')
        //     ->get();

        // return response()->json(['data' => $bibles]);
    }
}
