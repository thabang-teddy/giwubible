<?php

declare(strict_types=1);

namespace App\Models;

use App\Support\VerificationMail;
use Illuminate\Contracts\Auth\MustVerifyEmail;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;
use Laravel\Sanctum\HasApiTokens;

class User extends Authenticatable implements MustVerifyEmail
{
    use HasApiTokens, HasFactory, Notifiable;

    protected $fillable = ['name', 'email', 'password'];

    protected $hidden = ['password', 'remember_token'];

    protected $casts = [
        'email_verified_at' => 'datetime',
        'password' => 'hashed',
    ];

    public function bookmarks(): HasMany
    {
        return $this->hasMany(Bookmark::class);
    }

    /**
     * Applies validated profile changes from the web or the API.
     *
     * A new address is unverified by definition, so the old verification is
     * dropped and a fresh link goes out. Without this, moving the address
     * would silently inherit the previous one's verified state — which is the
     * whole guarantee verification exists to provide.
     *
     * @param  array<string, mixed>  $attributes
     */
    public function updateProfile(array $attributes): void
    {
        $emailChanged = isset($attributes['email']) && $attributes['email'] !== $this->email;

        $this->fill($attributes);

        if ($emailChanged) {
            $this->email_verified_at = null;
        }

        $this->save();

        if ($emailChanged) {
            VerificationMail::sendTo($this);
        }
    }
}
