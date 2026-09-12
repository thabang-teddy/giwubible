<?php

namespace Tests\Feature;

use Tests\TestCase;

class MaintenanceModeTest extends TestCase
{
    private string $envPath;

    private string $originalEnv;

    protected function setUp(): void
    {
        parent::setUp();

        $this->envPath = base_path('.env');
        $this->originalEnv = file_get_contents($this->envPath);
    }

    protected function tearDown(): void
    {
        file_put_contents($this->envPath, $this->originalEnv);

        parent::tearDown();
    }

    private function writeEnvFlags(bool $enabled, ?string $message = null): void
    {
        $contents = preg_replace(
            '/^MAINTENANCE_MODE=.*/m',
            'MAINTENANCE_MODE='.($enabled ? 'true' : 'false'),
            $this->originalEnv
        );

        if ($message !== null) {
            $contents = preg_replace(
                '/^#?\s*MAINTENANCE_MESSAGE=.*/m',
                'MAINTENANCE_MESSAGE="'.$message.'"',
                $contents
            );
        }

        file_put_contents($this->envPath, $contents);
    }

    public function test_web_pages_serve_normally_when_maintenance_mode_is_off(): void
    {
        $this->writeEnvFlags(false);

        $response = $this->get('/');

        $response->assertStatus(200);
    }

    public function test_web_pages_show_the_maintenance_page_when_enabled(): void
    {
        $this->writeEnvFlags(true);

        $response = $this->get('/');

        $response->assertStatus(503);
        $response->assertHeader('Retry-After', '3600');
        $response->assertSee('down for maintenance');
    }

    public function test_maintenance_message_is_configurable_via_env(): void
    {
        $this->writeEnvFlags(true, 'Back online at 3pm.');

        $response = $this->get('/');

        $response->assertSee('Back online at 3pm.');
    }

    public function test_api_routes_stay_reachable_during_maintenance(): void
    {
        $this->writeEnvFlags(true);

        $response = $this->get('/api/bibles');

        $response->assertStatus(200);
    }

    /**
     * The regression this whole feature exists to prevent: deploy/cpanel-deploy.sh
     * runs `artisan config:cache` on every release, which both freezes config()
     * at its deploy-time value and stops Laravel from loading .env at all on
     * later requests. Fake out exactly that stale state — config() and getenv()
     * both insist maintenance mode is off — and confirm the middleware still
     * follows what .env actually says, because it never consults either.
     */
    public function test_toggle_ignores_stale_config_and_process_env(): void
    {
        config(['app.maintenance_mode' => false]);
        putenv('MAINTENANCE_MODE=false');

        try {
            $this->writeEnvFlags(true);

            $this->get('/')->assertStatus(503);
        } finally {
            putenv('MAINTENANCE_MODE');
        }
    }
}
