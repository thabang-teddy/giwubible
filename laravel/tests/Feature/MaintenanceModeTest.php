<?php

namespace Tests\Feature;

use Tests\TestCase;

class MaintenanceModeTest extends TestCase
{
    public function test_web_pages_serve_normally_when_maintenance_mode_is_off(): void
    {
        config(['app.maintenance_mode' => false]);

        $response = $this->get('/');

        $response->assertStatus(200);
    }

    public function test_web_pages_show_the_maintenance_page_when_enabled(): void
    {
        config(['app.maintenance_mode' => true]);

        $response = $this->get('/');

        $response->assertStatus(503);
        $response->assertHeader('Retry-After', '3600');
        $response->assertSee('down for maintenance');
    }

    public function test_maintenance_message_is_configurable_via_env(): void
    {
        config([
            'app.maintenance_mode' => true,
            'app.maintenance_message' => 'Back online at 3pm.',
        ]);

        $response = $this->get('/');

        $response->assertSee('Back online at 3pm.');
    }

    public function test_api_routes_stay_reachable_during_maintenance(): void
    {
        config(['app.maintenance_mode' => true]);

        $response = $this->get('/api/bibles');

        $response->assertStatus(200);
    }
}
