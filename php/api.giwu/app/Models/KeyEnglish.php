<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class KeyEnglish extends Model
{
    protected $connection = 'bible_sqlite';
    protected $table = 'key_english';
    public $timestamps = false;
}
