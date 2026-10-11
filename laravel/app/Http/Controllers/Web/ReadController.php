<?php

declare(strict_types=1);

namespace App\Http\Controllers\Web;

use App\Http\Controllers\Controller;
use App\Repositories\BibleRepository;
use Illuminate\Http\Request;
use Inertia\Inertia;
use Inertia\Response;

class ReadController extends Controller
{
    /** The table a shared link falls back to when it names no translation. */
    private const DEFAULT_TABLE = 't_kjv';

    /**
     * The reader shell. Translations + book list arrive as initial props;
     * chapter verses and verse comparisons are fetched on-demand by the
     * client from the public JSON API (the in-page interactivity exception).
     */
    public function __invoke(BibleRepository $bibles): Response
    {
        return $this->page($bibles, null);
    }

    /**
     * A shared link: `/read/{book}/{chapter}?v={verse}`, where `{book}` is a
     * slug ("song-of-solomon") or a book number. This is what the apps' share
     * button hands out, so an unknown book or an empty chapter 404s rather
     * than opening the reader somewhere arbitrary.
     */
    public function chapter(
        Request $request,
        BibleRepository $bibles,
        string $book,
        int $chapter,
    ): Response {
        $row = $bibles->resolveBook($book);

        abort_if($row === null, 404);

        $table = $bibles->resolveTable((string) $request->query('bible', self::DEFAULT_TABLE))
            ?? self::DEFAULT_TABLE;

        abort_unless($bibles->hasChapter($table, (int) $row->b, $chapter), 404);

        $verse = (int) $request->query('v', '0');

        return $this->page($bibles, [
            'bible' => $table,
            'book' => (int) $row->b,
            'chapter' => $chapter,
            'verse' => $verse > 0 ? $verse : null,
        ]);
    }

    /**
     * Renders the reader shell. `$initial` is where a shared link asks it to
     * open; null leaves that choice to the browser's own stored position.
     *
     * @param  array{bible:string,book:int,chapter:int,verse:int|null}|null  $initial
     */
    private function page(BibleRepository $bibles, ?array $initial): Response
    {
        return Inertia::render('Read', [
            'bibles' => $bibles->versions(),
            'books' => $bibles->books(),
            'initial' => $initial,
        ]);
    }
}
