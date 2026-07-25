<?php

declare(strict_types=1);

namespace App\Repositories;

use App\Models\BibleVersionKey;
use App\Models\KeyEnglish;
use Illuminate\Support\Collection;
use Illuminate\Support\Facades\DB;

/**
 * Read-only access to the static bible-sqlite.db.
 *
 * Shared by the JSON API controllers (Flutter) and the Inertia web
 * controllers so the query logic lives in exactly one place.
 */
class BibleRepository
{
    /** All translations, ordered by abbreviation. */
    public function versions(): Collection
    {
        return BibleVersionKey::select('id', 'table', 'abbreviation', 'version', 'info_url')
            ->orderBy('abbreviation')
            ->get();
    }

    /** The 66-book list from key_english. */
    public function books(): Collection
    {
        return KeyEnglish::select('b', 'n', 't')
            ->orderBy('b')
            ->get();
    }

    /** Resolve a requested table name to a real table, or null if unknown. */
    public function resolveTable(string $requested): ?string
    {
        return BibleVersionKey::where('table', $requested)->value('table');
    }

    /** Resolve a requested table to its version metadata, or null if unknown. */
    public function resolveVersion(string $requested): ?object
    {
        return BibleVersionKey::where('table', $requested)
            ->select('table', 'abbreviation', 'version')
            ->first();
    }

    /** All verses of a chapter in the given (already-resolved) table. */
    public function chapter(string $table, int $book, int $chapter): Collection
    {
        return DB::connection('bible_sqlite')
            ->table($table)
            ->select('b', 'c', 'v', 't')
            ->where('b', $book)
            ->where('c', $chapter)
            ->orderBy('v')
            ->get();
    }

    /** A single verse row in the given (already-resolved) table, or null. */
    public function verse(string $table, int $book, int $chapter, int $verse): ?object
    {
        return DB::connection('bible_sqlite')
            ->table($table)
            ->where('b', $book)
            ->where('c', $chapter)
            ->where('v', $verse)
            ->first();
    }

    /**
     * The same verse across every translation that has it — one payload, so the
     * comparison panel can load all parallel versions in a single request.
     *
     * @return \Illuminate\Support\Collection<int, array{bible:string,abbreviation:string,version:string,text:string}>
     */
    public function comparisons(int $book, int $chapter, int $verse): Collection
    {
        return $this->versions()
            ->map(function ($version) use ($book, $chapter, $verse) {
                $row = $this->verse($version->table, $book, $chapter, $verse);

                return $row ? [
                    'bible' => $version->table,
                    'abbreviation' => $version->abbreviation,
                    'version' => $version->version,
                    'text' => $row->t,
                ] : null;
            })
            ->filter()
            ->values();
    }
}
