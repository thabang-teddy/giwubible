<?php

declare(strict_types=1);

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class Bookmark extends Model
{
    protected $fillable = ['user_id', 'bible', 'book', 'chapter', 'verse', 'text'];

    protected function casts(): array
    {
        return [
            'book' => 'integer',
            'chapter' => 'integer',
            'verse' => 'integer',
        ];
    }

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }
}
