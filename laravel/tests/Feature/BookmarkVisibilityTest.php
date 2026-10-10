<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Inertia\Testing\AssertableInertia as Assert;
use Tests\TestCase;

/**
 * Logged-out readers get no bookmark affordance at all: the shared props the
 * React components gate on must be empty, and the bookmark routes stay shut.
 */
class BookmarkVisibilityTest extends TestCase
{
    use RefreshDatabase;

    public function test_guest_reading_a_chapter_gets_no_user_and_no_bookmarks(): void
    {
        $this->get('/read')
            ->assertOk()
            ->assertInertia(fn (Assert $page) => $page
                ->component('Read')
                ->where('auth.user', null)
                ->where('bookmarks', []));
    }

    public function test_guest_cannot_create_a_bookmark(): void
    {
        $this->post('/bookmarks', [
            'bible' => 't_kjv',
            'book' => 1,
            'chapter' => 1,
            'verse' => 1,
            'text' => 'In the beginning God created the heaven and the earth.',
        ])->assertRedirect('/login');

        $this->assertDatabaseCount('bookmarks', 0);
    }

    public function test_signed_in_reader_still_receives_their_bookmarks(): void
    {
        $user = User::factory()->create();
        $user->bookmarks()->create([
            'bible' => 't_kjv',
            'book' => 1,
            'chapter' => 1,
            'verse' => 1,
            'text' => 'In the beginning God created the heaven and the earth.',
        ]);

        $this->actingAs($user)
            ->get('/read')
            ->assertOk()
            ->assertInertia(fn (Assert $page) => $page
                ->component('Read')
                ->where('auth.user.id', $user->id)
                ->has('bookmarks', 1, fn (Assert $bookmark) => $bookmark
                    ->where('bible', 't_kjv')
                    ->where('verse', 1)
                    ->etc()));
    }
}
