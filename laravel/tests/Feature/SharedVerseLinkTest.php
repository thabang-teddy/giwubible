<?php

namespace Tests\Feature;

use Inertia\Testing\AssertableInertia as Assert;
use Tests\TestCase;

/**
 * `/read/{book}/{chapter}?v={verse}` is the link the apps' share button hands
 * out, so it has to open the reader on exactly that passage — from a slug, a
 * book number, and with an unknown book or empty chapter refused rather than
 * silently opening Genesis 1.
 */
class SharedVerseLinkTest extends TestCase
{
    public function test_a_shared_link_opens_the_reader_on_that_verse(): void
    {
        $this->get('/read/john/3?v=16')
            ->assertOk()
            ->assertInertia(fn (Assert $page) => $page
                ->component('Read')
                ->where('initial.book', 43)
                ->where('initial.chapter', 3)
                ->where('initial.verse', 16)
                ->where('initial.bible', 't_kjv'));
    }

    public function test_a_shared_link_without_a_verse_opens_the_chapter(): void
    {
        $this->get('/read/song-of-solomon/2')
            ->assertOk()
            ->assertInertia(fn (Assert $page) => $page
                ->where('initial.book', 22)
                ->where('initial.chapter', 2)
                ->where('initial.verse', null));
    }

    public function test_a_book_number_works_as_well_as_a_slug(): void
    {
        $this->get('/read/62/4?v=8')
            ->assertOk()
            ->assertInertia(fn (Assert $page) => $page
                ->where('initial.book', 62)
                ->where('initial.chapter', 4)
                ->where('initial.verse', 8));
    }

    public function test_a_shared_link_can_name_the_translation(): void
    {
        $this->get('/read/john/3?v=16&bible=t_web')
            ->assertOk()
            ->assertInertia(fn (Assert $page) => $page->where('initial.bible', 't_web'));
    }

    public function test_an_unknown_book_or_chapter_is_not_found(): void
    {
        $this->get('/read/narnia/1')->assertNotFound();
        $this->get('/read/john/99')->assertNotFound();
    }

    public function test_the_plain_reader_is_left_to_its_stored_position(): void
    {
        $this->get('/read')
            ->assertOk()
            ->assertInertia(fn (Assert $page) => $page->where('initial', null));
    }
}
