<?php

namespace Tests\Feature;

use Inertia\Testing\AssertableInertia as Assert;
use Tests\TestCase;

class MergeTest extends TestCase
{
    public function test_home_renders_the_inertia_home_page(): void
    {
        $this->get('/')
            ->assertOk()
            ->assertInertia(fn (Assert $page) => $page->component('Home'));
    }

    public function test_read_page_receives_bibles_and_books_as_props(): void
    {
        $this->get('/read')
            ->assertOk()
            ->assertInertia(fn (Assert $page) => $page
                ->component('Read')
                ->has('bibles')
                ->has('books'));
    }

    public function test_login_page_renders(): void
    {
        $this->get('/login')
            ->assertOk()
            ->assertInertia(fn (Assert $page) => $page->component('Login'));
    }

    public function test_bookmarks_page_requires_authentication(): void
    {
        $this->get('/bookmarks')->assertRedirect('/login');
    }

    /** The Flutter app depends on this exact JSON envelope. */
    public function test_api_bibles_keeps_its_json_shape(): void
    {
        $this->getJson('/api/bibles')
            ->assertOk()
            ->assertJsonStructure(['data' => [['id', 'table', 'abbreviation', 'version']]]);
    }

    /** One call returns the verse across every translation (comparison panel). */
    public function test_api_verses_returns_all_translations_in_one_call(): void
    {
        $res = $this->getJson('/api/verses?book=1&chapter=1&verse=1')
            ->assertOk()
            ->assertJsonStructure(['data' => [['bible', 'abbreviation', 'version', 'text']]]);

        // Genesis 1:1 exists in more than one version, proving the single call
        // aggregates them rather than returning just one.
        $this->assertGreaterThan(1, count($res->json('data')));
    }

    public function test_api_books_keeps_its_json_shape(): void
    {
        // NB: the original endpoint also emits a quirky "t" => "t" field (a
        // SQLite double-quoted-identifier fallback on a non-existent column).
        // It is preserved byte-for-byte so the Flutter payload is unchanged;
        // b and n are the fields both clients actually use.
        $this->getJson('/api/books')
            ->assertOk()
            ->assertJsonStructure(['data' => [['b', 'n']]]);
    }
}
