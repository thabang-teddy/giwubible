<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Inertia\Testing\AssertableInertia as Assert;
use Tests\TestCase;

/**
 * The navbar's settings menu is the only account affordance now, and it offers
 * a guest nothing but Sign in. These assert the server side of that: the shared
 * prop the menu branches on, and the gate behind every link it offers a
 * signed-in reader.
 */
class SettingsMenuTest extends TestCase
{
    use RefreshDatabase;

    public function test_a_guest_reading_a_chapter_is_shared_no_user(): void
    {
        $this->get('/read')
            ->assertOk()
            ->assertInertia(fn (Assert $page) => $page
                ->component('Read')
                ->where('auth.user', null));
    }

    public function test_the_links_the_menu_withholds_from_a_guest_are_gated(): void
    {
        $this->get('/profile')->assertRedirect('/login');
        $this->get('/bookmarks')->assertRedirect('/login');
    }

    public function test_a_signed_in_reader_can_reach_every_link_the_menu_offers(): void
    {
        $user = User::factory()->create();

        $this->actingAs($user)->get('/profile')
            ->assertOk()
            ->assertInertia(fn (Assert $page) => $page->component('Profile'));

        $this->actingAs($user)->get('/bookmarks')
            ->assertOk()
            ->assertInertia(fn (Assert $page) => $page->component('Bookmarks'));

        $this->actingAs($user)->post('/logout')->assertRedirect();
        $this->assertGuest();
    }
}
