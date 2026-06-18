/**
 * Single source of truth for analytics event names and their property contracts.
 *
 * Naming conventions:
 *  - Event keys: snake_case, ≤32 chars, prefixed by domain (auth_/meal_/recipe_).
 *  - Property keys: snake_case, ≤16 chars (Umeng dimension limit).
 *  - Values: strings, numbers, booleans only — never objects/arrays.
 *
 * Each event is documented with the properties business code is expected to send.
 * Backend mirror: keep `AnalyticsEventController` happy by sending only these names.
 */
export const EVENTS = Object.freeze({
  // ── Auth ─────────────────────────────────────────
  AUTH_REGISTER_SUBMIT: 'auth_register_submit',     // { method }
  AUTH_REGISTER_SUCCESS: 'auth_register_success',   // { user_id }
  AUTH_REGISTER_FAIL: 'auth_register_fail',         // { error_code }
  AUTH_LOGIN_SUBMIT: 'auth_login_submit',           // { method: 'password'|'sms' }
  AUTH_LOGIN_SUCCESS: 'auth_login_success',         // { user_id, method }
  AUTH_LOGIN_FAIL: 'auth_login_fail',               // { method, error_code }
  AUTH_LOGOUT: 'auth_logout',                       // {}

  // ── Meal input ───────────────────────────────────
  MEAL_INPUT_TEXT_SUBMIT: 'meal_input_text_submit', // { length }
  MEAL_INPUT_VOICE_START: 'meal_input_voice_start', // {}
  MEAL_INPUT_VOICE_FINISH: 'meal_input_voice_fin',  // { duration_ms, ok }
  MEAL_INSPIRATION_TAP: 'meal_inspiration_tap',     // { count }

  // ── Meal recommendation flow ─────────────────────
  MEAL_PREFERENCE_COMPLETE: 'meal_pref_complete',   // { filled_fields }
  MEAL_RECOMMEND_SUBMIT: 'meal_recommend_submit',   // { dish_count, calories, staple }
  MEAL_RECOMMEND_RESULT: 'meal_recommend_result',   // { dish_count, duration_ms, ok }

  // ── Recipe interaction ───────────────────────────
  MEAL_RECIPE_VIEW: 'meal_recipe_view',             // { recipe_id }
  MEAL_RECIPE_LIKE: 'meal_recipe_like',             // { recipe_id }
  MEAL_RECIPE_DISLIKE: 'meal_recipe_dislike',       // { recipe_id }
  MEAL_RECIPE_SPEECH_START: 'meal_recipe_tts_start', // { recipe_id, step_count, rate, source }
  MEAL_RECIPE_SPEECH_PAUSE: 'meal_recipe_tts_pause', // { recipe_id, step_count, rate, source, step_index }
  MEAL_RECIPE_SPEECH_RESUME: 'meal_recipe_tts_resume', // { recipe_id, step_count, rate, source, step_index }
  MEAL_RECIPE_SPEECH_STEP: 'meal_recipe_tts_step',  // { recipe_id, step_count, rate, source, step_index }
  MEAL_RECIPE_SPEECH_FINISH: 'meal_recipe_tts_finish', // { recipe_id, step_count, rate, source, step_index }
  MEAL_RECIPE_SPEECH_FAIL: 'meal_recipe_tts_fail',  // { recipe_id, step_count, rate, source, step_index, error_code }

  // ── Notifications ────────────────────────────────
  NOTIF_PERM_PROMPT: 'notif_perm_prompt',           // { source }
  NOTIF_PERM_RESULT: 'notif_perm_result',           // { granted }
  NOTIF_SCHEDULED: 'notif_scheduled',               // { count }
  NOTIF_TAP: 'notif_tap',                           // { type, id }
  NOTIF_TOGGLE: 'notif_toggle',                     // { type, on }

  // ── Generic ──────────────────────────────────────
  SCREEN_VIEW: 'screen_view',                       // { screen }
});

export default EVENTS;
