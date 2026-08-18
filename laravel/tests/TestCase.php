<?php

namespace Tests;

use Illuminate\Foundation\Testing\TestCase as BaseTestCase;

abstract class TestCase extends BaseTestCase
{
    use CreatesApplication;

    /**
     * public/build is git-ignored — the bundle is produced by the `js` CI job
     * and by the `cpanel` job that builds the artefact, never by a clone. The
     * `php` job runs no npm, so @vite in the Inertia root view would throw
     * "Vite manifest not found" and every page test would see a 500.
     *
     * These tests assert status codes and Inertia props, not asset delivery.
     * That the bundle builds at all is the `js` job's assertion, and it is a
     * blocking check.
     */
    protected function setUp(): void
    {
        parent::setUp();

        $this->withoutVite();
    }
}
