import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
}

function json(status: number, body: Record<string, unknown>) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, 'Content-Type': 'application/json; charset=utf-8' },
  })
}

function normalizeEmail(raw: unknown) {
  return String(raw ?? '').trim().toLowerCase()
}

function normalizeKey(raw: unknown) {
  return String(raw ?? '')
    .trim()
    .toUpperCase()
    .replace(/[–—]/g, '-')
    .replace(/\s+/g, '')
}

function validEmail(email: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) && email.length <= 254
}

async function sha256(input: string) {
  const data = new TextEncoder().encode(input)
  const hash = await crypto.subtle.digest('SHA-256', data)
  return Array.from(new Uint8Array(hash)).map((b) => b.toString(16).padStart(2, '0')).join('')
}

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: corsHeaders })
  if (req.method !== 'POST') return json(405, { ok: false, code: 'method_not_allowed' })

  try {
    const body = await req.json().catch(() => ({}))
    const action = String(body.action ?? 'activate')
    const email = normalizeEmail(body.email)
    const key = normalizeKey(body.license_key)
    const appVersion = String(body.app_version ?? '').slice(0, 64)

    if (!['activate', 'status'].includes(action)) {
      return json(400, { ok: false, code: 'bad_action' })
    }
    if (!validEmail(email)) {
      return json(400, { ok: false, code: 'invalid_email' })
    }

    const match = key.match(/^HYA-(\d{4})-([A-F0-9]{5})-([A-F0-9]{5})$/)
    if (!match) return json(400, { ok: false, code: 'invalid_license' })

    const serial = Number(match[1])
    if (serial < 1 || serial > 1800) {
      return json(400, { ok: false, code: 'invalid_license' })
    }

    const supabaseUrl = Deno.env.get('SUPABASE_URL')
    const serviceRole = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')
    if (!supabaseUrl || !serviceRole) {
      return json(503, { ok: false, code: 'server_not_configured' })
    }

    const admin = createClient(supabaseUrl, serviceRole, {
      auth: { persistSession: false, autoRefreshToken: false },
    })

    const keyHash = await sha256(key)
    const { data: row, error } = await admin
      .from('licenses')
      .select('serial,status,bound_email,activated_at,activation_count')
      .eq('serial', serial)
      .eq('license_key_hash', keyHash)
      .maybeSingle()

    if (error) {
      console.error('license_lookup', error)
      return json(500, { ok: false, code: 'server_error' })
    }
    if (!row) return json(403, { ok: false, code: 'invalid_license' })
    if (row.status === 'revoked') return json(403, { ok: false, code: 'revoked' })

    const bound = normalizeEmail(row.bound_email)

    // Already activated: only the same email may use/recover the license.
    if (row.status === 'active') {
      if (bound !== email) {
        return json(409, {
          ok: false,
          code: 'already_bound',
          serial,
        })
      }

      await admin
        .from('licenses')
        .update({ last_seen_at: new Date().toISOString(), last_app_version: appVersion })
        .eq('serial', serial)

      return json(200, {
        ok: true,
        code: 'active',
        serial,
        email,
        activated_at: row.activated_at,
        server_time: new Date().toISOString(),
      })
    }

    // status call does not bind an unused license.
    if (action === 'status') {
      return json(200, {
        ok: false,
        code: 'unused',
        serial,
        server_time: new Date().toISOString(),
      })
    }

    // Atomic first activation. A concurrent request can only win once.
    const now = new Date().toISOString()
    const { data: activated, error: updateError } = await admin
      .from('licenses')
      .update({
        status: 'active',
        bound_email: email,
        activated_at: now,
        last_seen_at: now,
        activation_count: Number(row.activation_count ?? 0) + 1,
        last_app_version: appVersion,
      })
      .eq('serial', serial)
      .eq('status', 'unused')
      .is('bound_email', null)
      .select('serial,status,bound_email,activated_at')
      .maybeSingle()

    if (updateError) {
      console.error('license_activate', updateError)
      return json(500, { ok: false, code: 'server_error' })
    }

    if (activated) {
      return json(200, {
        ok: true,
        code: 'activated',
        serial,
        email,
        activated_at: activated.activated_at,
        server_time: new Date().toISOString(),
      })
    }

    // Resolve an activation race by reading the final binding.
    const { data: finalRow } = await admin
      .from('licenses')
      .select('status,bound_email,activated_at')
      .eq('serial', serial)
      .single()

    if (finalRow?.status === 'active' && normalizeEmail(finalRow.bound_email) === email) {
      return json(200, {
        ok: true,
        code: 'active',
        serial,
        email,
        activated_at: finalRow.activated_at,
        server_time: new Date().toISOString(),
      })
    }

    return json(409, { ok: false, code: 'already_bound', serial })
  } catch (e) {
    console.error('license_api_uncaught', e)
    return json(500, { ok: false, code: 'server_error' })
  }
})
