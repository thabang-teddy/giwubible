<?php

declare(strict_types=1);

namespace App\Http\Controllers\Web;

use App\Http\Controllers\Controller;
use App\Repositories\BibleRepository;
use Inertia\Inertia;
use Inertia\Response;

class ReadController extends Controller
{
    /**
     * The reader shell. Translations + book list arrive as initial props;
     * chapter verses and verse comparisons are fetched on-demand by the
     * client from the public JSON API (the in-page interactivity exception).
     */
    public function __invoke(BibleRepository $bibles): Response
    {
        return Inertia::render('Read', [
            'bibles' => $bibles->versions(),
            'books' => $bibles->books(),
        ]);
    }
}
