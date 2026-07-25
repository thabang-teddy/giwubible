<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class BibleVersionKey extends Model
{
    protected $connection = 'bible_sqlite';
    protected $table = 'bible_version_key';
    public $timestamps = false;
}
