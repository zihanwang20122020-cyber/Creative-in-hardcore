#!/usr/bin/env python3
"""Generates the built-in emote files shipped inside the mod jar.

Run from this directory:

    python3 generate_emotes.py

Every entry below is sampled from the continuous curves in ``archetypes.py`` into dense keyframes,
so each emote arrives with real motion on several body parts rather than a pose and a hold.
"""

import json
import os

import archetypes as A
from emote_dsl import sample_track

OUT_DIR = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                       "assets", "emotestudio", "emotes")

AUTHOR = "Emote Studio"

# id, display name, category, description, archetype, kwargs, length (ticks), loop, samples, tags
CATALOGUE = [
    # ---------------------------------------------------------------- greetings
    ("wave", "Wave", "greeting", "A friendly hand wave at shoulder height.",
     A.wave, {}, 30, True, 20, ["hello", "hi"]),
    ("wave_slow", "Slow Wave", "greeting", "A calm, unhurried wave goodbye.",
     A.wave, {"freq": 1.5, "swing": 34.0}, 46, True, 24, ["bye", "calm"]),
    ("wave_fast", "Excited Wave", "greeting", "A quick, energetic wave with a bouncing shoulder.",
     A.wave, {"freq": 5.0, "swing": 22.0, "lean": 7.0}, 22, True, 22, ["excited"]),
    ("wave_left", "Left Wave", "greeting", "The same wave, led with the left hand.",
     A.wave, {"side": "left"}, 30, True, 20, ["hello"]),
    ("wave_low", "Little Wave", "greeting", "A shy wave kept down by the hip.",
     A.wave, {"raise_angle": -58.0, "swing": 18.0, "freq": 2.5}, 32, True, 20, ["shy"]),
    ("wave_both", "Both Hands Wave", "greeting", "Both arms overhead, impossible to miss.",
     A.big_wave, {}, 32, True, 22, ["hello", "attention"]),
    ("wave_signal", "Signal Over Here", "greeting", "Wide overhead sweeps to call someone across.",
     A.big_wave, {"freq": 1.4}, 44, True, 26, ["attention"]),
    ("salute", "Salute", "greeting", "A crisp military salute, held and released.",
     A.salute, {}, 40, False, 22, ["respect"]),
    ("salute_left", "Left Salute", "greeting", "A salute given with the left hand.",
     A.salute, {"side": "left"}, 40, False, 22, ["respect"]),
    ("salute_long", "Long Salute", "greeting", "A salute held far longer than regulation.",
     A.salute, {"hold": 0.65}, 60, False, 26, ["respect"]),
    ("bow", "Bow", "greeting", "A polite bow from the waist.",
     A.bow, {}, 44, False, 24, ["respect", "polite"]),
    ("bow_deep", "Deep Bow", "greeting", "A long, low bow of real deference.",
     A.bow, {"depth": 76.0, "hold": 0.45}, 62, False, 28, ["respect"]),
    ("bow_court", "Court Bow", "greeting", "A courtly bow with one arm swept aside.",
     A.bow, {"depth": 58.0, "arm_style": "sweep"}, 52, False, 26, ["fancy"]),
    ("bow_formal", "Formal Bow", "greeting", "A formal bow with both hands crossed at the chest.",
     A.bow, {"depth": 48.0, "arm_style": "cross"}, 50, False, 26, ["polite"]),
    ("handshake", "Handshake", "greeting", "An offered hand, pumping up and down.",
     A.handshake, {}, 32, True, 20, ["deal"]),
    ("handshake_firm", "Firm Handshake", "greeting", "A vigorous handshake that will not let go.",
     A.handshake, {"freq": 5.0}, 26, True, 22, ["deal"]),
    ("point_forward", "Point Ahead", "greeting", "Points straight ahead with a small insistent jab.",
     A.point, {}, 30, True, 18, ["look", "there"]),
    ("point_up", "Point Up", "greeting", "Points at the sky.",
     A.point, {"pitch": -168.0}, 32, True, 18, ["look"]),
    ("point_down", "Point Down", "greeting", "Points firmly at the ground.",
     A.point, {"pitch": -18.0, "jab": 9.0}, 30, True, 18, ["here"]),
    ("point_side", "Point Aside", "greeting", "Points off to one side with a turn of the head.",
     A.point, {"yaw": 46.0}, 32, True, 18, ["there"]),
    ("nod_yes", "Nod Yes", "greeting", "A clear, agreeable nod.",
     A.nod, {}, 26, True, 18, ["yes", "agree"]),
    ("nod_slow", "Slow Nod", "greeting", "A thoughtful, slow nod of agreement.",
     A.nod, {"freq": 1.0, "amp": 21.0}, 44, True, 22, ["yes"]),
    ("shake_no", "Shake No", "greeting", "A firm shake of the head.",
     A.nod, {"agree": False}, 28, True, 18, ["no", "disagree"]),
    ("shake_no_fast", "Absolutely Not", "greeting", "A rapid, emphatic refusal.",
     A.nod, {"agree": False, "freq": 4.0, "amp": 14.0}, 22, True, 20, ["no"]),
    ("shrug", "Shrug", "greeting", "Shoulders and palms up: no idea.",
     A.shrug, {}, 40, False, 22, ["dunno"]),
    ("shrug_long", "Long Shrug", "greeting", "A shrug held long enough to be a statement.",
     A.shrug, {"hold": 0.6}, 56, False, 26, ["dunno"]),
    ("clap", "Clap", "greeting", "Steady applause.",
     A.clap, {}, 24, True, 20, ["applause"]),
    ("clap_fast", "Fast Clap", "greeting", "Rapid, delighted clapping.",
     A.clap, {"freq": 6.0, "spread": 22.0}, 20, True, 20, ["applause"]),
    ("clap_slow", "Slow Clap", "greeting", "A sarcastic, deliberate slow clap.",
     A.slow_clap, {}, 44, True, 24, ["sarcasm"]),
    ("clap_overhead", "Overhead Clap", "greeting", "Clapping above the head, festival style.",
     A.applause_over_head, {}, 28, True, 22, ["applause", "cheer"]),
    ("clap_ovation", "Standing Ovation", "greeting", "Long overhead applause with a bounce.",
     A.applause_over_head, {"freq": 2.0}, 36, True, 24, ["applause"]),
    ("facepalm", "Facepalm", "emotion", "Palm meets face, slowly.",
     A.facepalm, {}, 46, False, 24, ["shame", "oops"]),
    ("facepalm_double", "Double Facepalm", "emotion", "One hand was not enough.",
     A.facepalm, {"hold": 0.62}, 60, False, 28, ["shame"]),

    # ---------------------------------------------------------------- dances
    ("dance_bounce", "Bounce", "dance", "A simple two-step bounce with swinging arms.",
     A.dance_bounce, {}, 24, True, 24, ["party"]),
    ("dance_bounce_fast", "Fast Bounce", "dance", "The bounce, doubled in tempo.",
     A.dance_bounce, {"freq": 3.0, "height": 3.0}, 18, True, 24, ["party"]),
    ("dance_bounce_wide", "Wide Bounce", "dance", "A bounce with big, loose arm swings.",
     A.dance_bounce, {"arm": 68.0, "hip": 14.0}, 28, True, 26, ["party"]),
    ("dance_step", "Two Step", "dance", "A restrained bounce, mostly in the hips.",
     A.dance_bounce, {"arm": 22.0, "height": 1.4, "hip": 12.0}, 30, True, 24, ["chill"]),
    ("dance_jump", "Jump Dance", "dance", "Nearly leaving the ground on every beat.",
     A.dance_bounce, {"height": 4.4, "arm": 58.0, "freq": 2.0}, 24, True, 26, ["party"]),
    ("dance_sway", "Sway", "dance", "A slow, wide sway from side to side.",
     A.dance_sway, {}, 48, True, 26, ["chill", "slow"]),
    ("dance_sway_fast", "Quick Sway", "dance", "The sway at twice the tempo.",
     A.dance_sway, {"freq": 2.0, "amp": 12.0}, 26, True, 24, ["party"]),
    ("dance_slow", "Slow Dance", "dance", "A gentle, close slow dance.",
     A.dance_sway, {"freq": 0.5, "amp": 11.0, "arm_amp": 20.0}, 72, True, 30, ["romantic", "slow"]),
    ("dance_wave_arms", "Arm Waves", "dance", "Sway with big alternating arm circles.",
     A.dance_sway, {"arm_amp": 62.0, "amp": 9.0}, 40, True, 28, ["party"]),
    ("dance_groove", "Groove", "dance", "A loose, unhurried groove.",
     A.dance_sway, {"freq": 1.5, "amp": 18.0, "arm_amp": 42.0}, 32, True, 26, ["party"]),
    ("dance_spin", "Spin", "dance", "A full turn with arms flung wide.",
     A.dance_spin, {}, 30, True, 26, ["twirl"]),
    ("dance_spin_double", "Double Spin", "dance", "Two turns without stopping.",
     A.dance_spin, {"turns": 2.0}, 34, True, 30, ["twirl"]),
    ("dance_twirl", "Twirl", "dance", "A light twirl on the toes.",
     A.dance_spin, {"turns": 1.0, "arm": -140.0, "hop": 2.4, "tilt": 12.0}, 36, True, 28, ["twirl"]),
    ("dance_pirouette", "Pirouette", "dance", "A ballet turn with arms held overhead.",
     A.dance_spin, {"turns": 2.0, "arm": -168.0, "tilt": 4.0, "hop": 0.8}, 40, True, 30, ["ballet"]),
    ("moonwalk", "Moonwalk", "dance", "Sliding backwards while leaning forward — the classic.",
     A.moonwalk, {}, 40, True, 28, ["classic", "slide"]),
    ("moonwalk_slow", "Slow Moonwalk", "dance", "The moonwalk stretched out and smoothed over.",
     A.moonwalk, {"freq": 1.2, "distance": 12.0}, 60, True, 32, ["slide"]),
    ("moonwalk_fast", "Quick Moonwalk", "dance", "A hurried moonwalk with sharper footwork.",
     A.moonwalk, {"freq": 3.5, "distance": 7.0}, 26, True, 26, ["slide"]),
    ("moonwalk_side", "Side Glide", "dance", "A gliding step with a strong forward lean.",
     A.moonwalk, {"lean": 20.0, "distance": 6.0, "freq": 2.5}, 34, True, 26, ["slide"]),
    ("floss", "Floss", "dance", "Arms one way, hips the other.",
     A.floss, {}, 24, True, 24, ["meme"]),
    ("floss_fast", "Turbo Floss", "dance", "The floss, at an unreasonable speed.",
     A.floss, {"freq": 3.5}, 18, True, 24, ["meme"]),
    ("floss_wide", "Wide Floss", "dance", "A floss with exaggerated reach.",
     A.floss, {"amp": 42.0, "arm_swing": 60.0}, 28, True, 26, ["meme"]),
    ("macarena", "Macarena", "dance", "The full eight-count arm routine.",
     A.macarena, {}, 64, True, 32, ["classic", "party"]),
    ("macarena_fast", "Fast Macarena", "dance", "The routine at party tempo.",
     A.macarena, {"freq": 2.0}, 40, True, 30, ["party"]),
    ("disco", "Disco", "dance", "Point to the sky, point to the floor, repeat.",
     A.disco, {}, 28, True, 24, ["retro"]),
    ("disco_fever", "Disco Fever", "dance", "Disco with everything turned up.",
     A.disco, {"freq": 3.0, "reach": -170.0}, 22, True, 26, ["retro"]),
    ("disco_smooth", "Smooth Disco", "dance", "A laid-back disco point.",
     A.disco, {"freq": 1.2, "reach": -132.0}, 44, True, 28, ["retro", "chill"]),
    ("robot", "Robot", "dance", "Sharp, stepped movements with no easing at all.",
     A.robot, {}, 32, True, 32, ["mechanical"]),
    ("robot_fast", "Malfunction", "dance", "The robot, glitching.",
     A.robot, {"steps_count": 16, "amp": 84.0}, 24, True, 32, ["mechanical", "glitch"]),
    ("robot_slow", "Heavy Robot", "dance", "A slow, heavy mechanical march.",
     A.robot, {"steps_count": 6, "amp": 58.0}, 48, True, 30, ["mechanical"]),
    ("breakdance", "Breakdance Spin", "dance", "Low spin on one hand.",
     A.breakdance_spin, {}, 34, True, 30, ["street"]),
    ("breakdance_fast", "Power Spin", "dance", "A faster, wilder floor spin.",
     A.breakdance_spin, {"turns": 4.0, "freq": 3.0}, 28, True, 32, ["street"]),
    ("worm", "The Worm", "dance", "A body wave travelling along the floor.",
     A.worm, {}, 36, True, 30, ["street", "floor"]),
    ("worm_fast", "Fast Worm", "dance", "The worm, considerably more caffeinated.",
     A.worm, {"freq": 2.0, "amplitude": 4.4}, 24, True, 28, ["street"]),
    ("hip_shake", "Hip Shake", "dance", "Quick hip movement with raised arms.",
     A.hip_shake, {}, 20, True, 22, ["party"]),
    ("hip_sway", "Hip Sway", "dance", "A slower, wider hip motion.",
     A.hip_shake, {"freq": 2.0, "amp": 24.0}, 32, True, 24, ["party"]),
    ("hip_bump", "Hip Bump", "dance", "Short, punchy hip accents.",
     A.hip_shake, {"freq": 6.0, "amp": 12.0}, 18, True, 24, ["party"]),
    ("air_guitar", "Air Guitar", "dance", "A furious solo on an instrument that is not there.",
     A.air_guitar, {}, 28, True, 26, ["music", "rock"]),
    ("air_guitar_solo", "Guitar Solo", "dance", "The extended, self-indulgent version.",
     A.air_guitar, {"freq": 5.0}, 22, True, 26, ["music", "rock"]),
    ("air_drums", "Air Drums", "dance", "Both arms hammering an invisible kit.",
     A.drumming, {}, 20, True, 24, ["music"]),
    ("air_drums_solo", "Drum Solo", "dance", "A drum fill that never resolves.",
     A.drumming, {"freq": 7.0}, 16, True, 24, ["music"]),
    ("dj", "DJ Scratch", "dance", "One hand on the deck, one in the air.",
     A.dj_scratch, {}, 24, True, 24, ["music"]),
    ("dj_drop", "Drop the Beat", "dance", "Working the deck ahead of the drop.",
     A.dj_scratch, {"freq": 5.0}, 18, True, 24, ["music"]),
    ("dab", "Dab", "dance", "The dab, held just long enough.",
     A.dab, {}, 26, False, 20, ["meme"]),
    ("dab_left", "Left Dab", "dance", "A dab to the other side.",
     A.dab, {"side": "left"}, 26, False, 20, ["meme"]),
    ("kick_dance", "Kick Step", "dance", "Alternating front kicks in time.",
     A.kick_dance, {}, 32, True, 26, ["party"]),
    ("kick_dance_high", "High Kick Step", "dance", "The kick step, considerably higher.",
     A.kick_dance, {"kick": -108.0}, 36, True, 28, ["party"]),
    ("can_can", "Can-Can", "dance", "Fast alternating high kicks.",
     A.kick_dance, {"kick": -122.0, "freq": 2.0}, 26, True, 28, ["classic"]),
    ("shuffle", "Shuffle", "dance", "Quick shuffling footwork.",
     A.shuffle, {}, 22, True, 24, ["street"]),
    ("shuffle_fast", "Running Man", "dance", "Shuffle footwork at a sprint.",
     A.shuffle, {"freq": 5.0}, 16, True, 24, ["street"]),
    ("shuffle_slow", "Lazy Shuffle", "dance", "Barely-there shuffling.",
     A.shuffle, {"freq": 1.5}, 40, True, 26, ["chill"]),

    # ---------------------------------------------------------------- floating
    ("float", "Float", "float", "Hovering just off the ground, arms drifting.",
     A.float_idle, {}, 80, True, 32, ["hover", "calm"]),
    ("float_high", "High Float", "float", "Hovering well above the ground.",
     A.float_idle, {"height": 15.0, "drift": 3.2}, 80, True, 32, ["hover"]),
    ("float_low", "Low Float", "float", "A gentle hover barely off the floor.",
     A.float_idle, {"height": 3.0, "drift": 1.2}, 72, True, 30, ["hover", "calm"]),
    ("float_fast", "Restless Float", "float", "A hover that will not settle.",
     A.float_idle, {"freq": 1.4, "drift": 3.0}, 40, True, 30, ["hover"]),
    ("float_star", "Star Float", "float", "Hovering with the arms spread wide.",
     A.float_idle, {"arm_out": 82.0, "height": 9.0}, 76, True, 32, ["hover"]),
    ("heaven", "Heaven", "float", "Rising with open arms and a tilted head.",
     A.heaven, {}, 96, False, 34, ["ascend", "divine"]),
    ("heaven_slow", "Ascension", "float", "A long, unhurried rise skyward.",
     A.heaven, {"height": 18.0, "freq": 0.25}, 140, False, 38, ["ascend", "divine"]),
    ("heaven_loop", "Blessed", "float", "Held aloft, arms open, endlessly.",
     A.heaven, {"height": 6.0, "rise": False}, 90, True, 32, ["divine"]),
    ("ascend", "Take Off", "float", "Rising while slowly turning.",
     A.ascend, {}, 90, False, 32, ["ascend"]),
    ("ascend_spin", "Spiral Up", "float", "A full spiral climb.",
     A.ascend, {"spin": 3.0, "height": 22.0}, 110, False, 36, ["ascend"]),
    ("ascend_short", "Lift Off", "float", "A short, sharp lift.",
     A.ascend, {"height": 9.0, "spin": 0.5}, 48, False, 26, ["ascend"]),
    ("fly_hero", "Hero Flight", "float", "One fist forward, cape logic optional.",
     A.superhero_fly, {}, 60, True, 28, ["fly", "hero"]),
    ("fly_hero_high", "Sky Patrol", "float", "Hero flight, much higher up.",
     A.superhero_fly, {"height": 18.0}, 60, True, 28, ["fly", "hero"]),
    ("fly_dive", "Dive", "float", "A fast, low hero dive.",
     A.superhero_fly, {"height": 4.0, "freq": 1.4}, 36, True, 28, ["fly"]),
    ("swim_air", "Air Swim", "float", "Front crawl through nothing at all.",
     A.swim_air, {}, 44, True, 30, ["silly", "fly"]),
    ("swim_air_fast", "Sprint Swim", "float", "Air swimming at racing pace.",
     A.swim_air, {"freq": 2.0}, 28, True, 30, ["silly"]),
    ("meditate", "Meditate", "float", "Cross-legged and hovering, perfectly still.",
     A.levitate_meditate, {}, 120, True, 32, ["calm", "zen"]),
    ("meditate_high", "Deep Meditation", "float", "Hovering higher, drifting slower.",
     A.levitate_meditate, {"height": 12.0, "freq": 0.2}, 160, True, 34, ["calm", "zen"]),
    ("meditate_low", "Sit and Breathe", "float", "Barely off the ground, just breathing.",
     A.levitate_meditate, {"height": 1.5, "freq": 0.45}, 100, True, 30, ["calm"]),
    ("ghost", "Ghost", "float", "Drifting sideways with limp arms.",
     A.ghost_drift, {}, 100, True, 32, ["spooky"]),
    ("ghost_fast", "Haunting", "float", "A more agitated drift.",
     A.ghost_drift, {"freq": 0.9, "height": 8.0}, 60, True, 30, ["spooky"]),
    ("ghost_low", "Wraith", "float", "A low, creeping drift.",
     A.ghost_drift, {"height": 2.0, "freq": 0.3}, 120, True, 32, ["spooky"]),
    ("moon_jump", "Moon Jump", "float", "Long, low-gravity hops.",
     A.moon_jump, {}, 64, True, 30, ["space"]),
    ("moon_jump_high", "Low Gravity", "float", "Hops that go alarmingly high.",
     A.moon_jump, {"height": 22.0, "freq": 0.35}, 90, True, 32, ["space"]),
    ("moon_jump_fast", "Bunny Hops", "float", "Quick repeated hops.",
     A.moon_jump, {"height": 7.0, "freq": 1.2}, 34, True, 28, ["space"]),

    # ---------------------------------------------------------------- poses
    ("sit", "Sit", "pose", "Sitting on the ground, legs forward.",
     A.sit, {}, 80, True, 24, ["rest"]),
    ("sit_chair", "Sit on Chair", "pose", "Seated as if there were a chair.",
     A.sit, {"style": "chair"}, 80, True, 24, ["rest"]),
    ("sit_cross", "Cross-Legged", "pose", "Sitting cross-legged, hands on knees.",
     A.sit, {"style": "cross"}, 90, True, 24, ["rest", "zen"]),
    ("sit_relaxed", "Lounge", "pose", "A slouched, unhurried sit.",
     A.sit, {"breathe": 3.0, "freq": 0.22}, 110, True, 26, ["rest"]),
    ("lie_back", "Lie Down", "pose", "Flat on the back, arms out.",
     A.lie_down, {}, 100, True, 22, ["rest"]),
    ("lie_front", "Lie Face Down", "pose", "Face down, arms above the head.",
     A.lie_down, {"face": "down"}, 100, True, 22, ["rest"]),
    ("lie_relaxed", "Stargaze", "pose", "On the back, breathing slowly.",
     A.lie_down, {"freq": 0.18}, 140, True, 24, ["rest", "calm"]),
    ("sleep", "Sleep", "pose", "Curled up asleep, with slow breathing.",
     A.sleep, {}, 140, True, 24, ["rest"]),
    ("sleep_deep", "Deep Sleep", "pose", "Very slow, very asleep.",
     A.sleep, {"freq": 0.15, "snore": 3.6}, 200, True, 26, ["rest"]),
    ("kneel", "Kneel", "pose", "Down on one knee.",
     A.kneel, {}, 70, True, 20, ["respect"]),
    ("kneel_pray", "Pray", "pose", "Kneeling with hands together.",
     A.kneel, {"pray": True}, 90, True, 22, ["respect", "divine"]),
    ("kneel_low", "Kneel Low", "pose", "A deeper kneel, head bowed.",
     A.kneel, {"depth": 1.5, "pray": True}, 90, True, 22, ["respect"]),
    ("t_pose", "T-Pose", "pose", "Arms straight out. Asserting dominance.",
     A.t_pose, {}, 60, True, 12, ["meme"]),
    ("t_pose_spin", "Spinning T-Pose", "pose", "The T-pose, rotating.",
     A.t_pose, {"spin": 1.0}, 60, True, 28, ["meme"]),
    ("t_pose_fast_spin", "T-Pose Drill", "pose", "Three full rotations.",
     A.t_pose, {"spin": 3.0}, 60, True, 32, ["meme"]),
    ("flex", "Flex", "pose", "Both arms up, showing off.",
     A.flex, {}, 44, True, 22, ["strong"]),
    ("flex_right", "Right Flex", "pose", "One-armed flex.",
     A.flex, {"side": "right"}, 44, True, 22, ["strong"]),
    ("flex_left", "Left Flex", "pose", "The other one-armed flex.",
     A.flex, {"side": "left"}, 44, True, 22, ["strong"]),
    ("flex_pump", "Muscle Pump", "pose", "A flex with a visible pump.",
     A.flex, {"freq": 2.5, "pump": 16.0}, 28, True, 24, ["strong"]),
    ("arms_crossed", "Arms Crossed", "pose", "Arms folded, waiting.",
     A.arms_crossed, {}, 90, True, 20, ["wait"]),
    ("arms_crossed_tap", "Impatient", "pose", "Arms folded and one foot tapping.",
     A.arms_crossed, {"tap": 14.0}, 60, True, 26, ["wait", "annoyed"]),
    ("arms_crossed_cool", "Unimpressed", "pose", "Arms folded, very still.",
     A.arms_crossed, {"freq": 0.2}, 120, True, 20, ["wait"]),
    ("hands_hips", "Hands on Hips", "pose", "A confident, planted stance.",
     A.hands_on_hips, {}, 80, True, 20, ["confident"]),
    ("hands_hips_scan", "Surveying", "pose", "Hands on hips, looking around.",
     A.hands_on_hips, {"freq": 0.8}, 60, True, 24, ["confident"]),
    ("think", "Think", "pose", "Hand to the chin, working it out.",
     A.think, {}, 70, True, 22, ["idea"]),
    ("think_hard", "Deep Thought", "pose", "Thinking, but harder.",
     A.think, {"freq": 1.4}, 44, True, 24, ["idea"]),
    ("think_slow", "Ponder", "pose", "A very long think.",
     A.think, {"freq": 0.3}, 120, True, 24, ["idea"]),
    ("stretch", "Stretch", "pose", "A full overhead stretch.",
     A.stretch, {}, 56, False, 24, ["tired"]),
    ("stretch_long", "Big Stretch", "pose", "A stretch held for a while.",
     A.stretch, {"reach": -178.0}, 76, False, 28, ["tired"]),
    ("stretch_side", "Side Stretch", "pose", "A shorter, easier stretch.",
     A.stretch, {"reach": -140.0}, 44, False, 22, ["tired"]),
    ("yawn", "Yawn", "pose", "A wide yawn with an arm up.",
     A.yawn, {}, 52, False, 24, ["tired"]),
    ("yawn_big", "Huge Yawn", "pose", "A yawn that takes its time.",
     A.yawn, {"freq": 0.25}, 76, False, 28, ["tired"]),
    ("idle_breathe", "Breathe", "pose", "A quiet idle with real breathing.",
     A.breathe_idle, {}, 100, True, 24, ["idle", "calm"]),
    ("idle_calm", "Rest", "pose", "A slower, deeper idle.",
     A.breathe_idle, {"freq": 0.18, "depth": 3.0}, 140, True, 26, ["idle", "calm"]),
    ("idle_alert", "Alert Idle", "pose", "A tighter, more watchful idle.",
     A.breathe_idle, {"freq": 0.6, "depth": 1.2}, 60, True, 24, ["idle"]),

    # ---------------------------------------------------------------- emotion
    ("cry", "Cry", "emotion", "Hands to the face, shoulders shaking.",
     A.cry, {}, 44, True, 26, ["sad"]),
    ("cry_hard", "Sob", "emotion", "Crying that will not be consoled.",
     A.cry, {"freq": 2.6}, 30, True, 26, ["sad"]),
    ("cry_quiet", "Quiet Tears", "emotion", "A restrained, quiet cry.",
     A.cry, {"freq": 0.7}, 70, True, 26, ["sad"]),
    ("laugh", "Laugh", "emotion", "Head back, genuine laughter.",
     A.laugh, {}, 24, True, 24, ["happy"]),
    ("laugh_hard", "Howl", "emotion", "Uncontrollable laughter.",
     A.laugh, {"freq": 5.0}, 18, True, 24, ["happy"]),
    ("laugh_soft", "Chuckle", "emotion", "A short, quiet chuckle.",
     A.laugh, {"freq": 1.6}, 36, True, 24, ["happy"]),
    ("rage", "Rage", "emotion", "Fists shaking, feet stamping.",
     A.rage, {}, 24, True, 26, ["angry"]),
    ("rage_hard", "Fury", "emotion", "Rage with nothing held back.",
     A.rage, {"freq": 5.0}, 18, True, 26, ["angry"]),
    ("rage_slow", "Seething", "emotion", "Anger kept barely under control.",
     A.rage, {"freq": 1.2}, 44, True, 26, ["angry"]),
    ("cheer", "Cheer", "emotion", "Arms up, jumping for joy.",
     A.cheer, {}, 26, True, 24, ["happy", "win"]),
    ("cheer_big", "Victory", "emotion", "A full celebration.",
     A.cheer, {"height": 4.4, "freq": 1.6}, 34, True, 26, ["win"]),
    ("cheer_fast", "Hype", "emotion", "Fast, relentless celebrating.",
     A.cheer, {"freq": 4.0, "height": 1.8}, 18, True, 24, ["win"]),
    ("cheer_calm", "Quiet Win", "emotion", "A restrained celebration.",
     A.cheer, {"freq": 0.9, "height": 1.2}, 44, True, 24, ["win"]),
    ("defeat", "Defeat", "emotion", "Head down, arms limp.",
     A.defeat, {}, 90, True, 22, ["sad", "lose"]),
    ("defeat_deep", "Despair", "emotion", "Defeat, sunk even lower.",
     A.defeat, {"freq": 0.2}, 130, True, 24, ["sad"]),
    ("scared", "Scared", "emotion", "Hands up, trembling.",
     A.scared, {}, 26, True, 26, ["fear"]),
    ("scared_panic", "Panic", "emotion", "Shaking far too much.",
     A.scared, {"freq": 10.0}, 18, True, 28, ["fear"]),
    ("confused", "Confused", "emotion", "Palms up, head tilting.",
     A.confused, {}, 60, True, 24, ["dunno"]),
    ("confused_lost", "Completely Lost", "emotion", "Confusion at a larger scale.",
     A.confused, {"freq": 1.6}, 34, True, 24, ["dunno"]),

    # ---------------------------------------------------------------- exercise
    ("jumping_jacks", "Jumping Jacks", "sport", "Standard jumping jacks.",
     A.jumping_jacks, {}, 24, True, 26, ["fitness"]),
    ("jumping_jacks_fast", "Fast Jacks", "sport", "Jumping jacks at pace.",
     A.jumping_jacks, {"freq": 2.0}, 16, True, 26, ["fitness"]),
    ("jumping_jacks_slow", "Warm Up", "sport", "Slow, careful jacks.",
     A.jumping_jacks, {"freq": 0.6}, 40, True, 26, ["fitness"]),
    ("push_ups", "Push Ups", "sport", "Steady push ups.",
     A.push_ups, {}, 30, True, 24, ["fitness"]),
    ("push_ups_fast", "Fast Push Ups", "sport", "Push ups at speed.",
     A.push_ups, {"freq": 2.0}, 18, True, 24, ["fitness"]),
    ("push_ups_slow", "Slow Push Ups", "sport", "Slow, controlled push ups.",
     A.push_ups, {"freq": 0.5}, 56, True, 26, ["fitness"]),
    ("sit_ups", "Sit Ups", "sport", "Sit ups from flat on the back.",
     A.sit_ups, {}, 32, True, 24, ["fitness"]),
    ("sit_ups_fast", "Crunches", "sport", "Short, fast crunches.",
     A.sit_ups, {"freq": 2.2}, 18, True, 24, ["fitness"]),
    ("squats", "Squats", "sport", "Full squats with the arms forward.",
     A.squats, {}, 32, True, 24, ["fitness"]),
    ("squats_deep", "Deep Squats", "sport", "Squats taken all the way down.",
     A.squats, {"depth": 9.0}, 40, True, 26, ["fitness"]),
    ("squats_fast", "Pulse Squats", "sport", "Quick squat pulses.",
     A.squats, {"freq": 2.5, "depth": 3.5}, 18, True, 24, ["fitness"]),
    ("run_place", "Run in Place", "sport", "Running without going anywhere.",
     A.run_in_place, {}, 20, True, 26, ["fitness"]),
    ("run_place_fast", "Sprint", "sport", "Sprinting on the spot.",
     A.run_in_place, {"freq": 5.0, "knee": 68.0}, 14, True, 26, ["fitness"]),
    ("jog_place", "Jog", "sport", "An easy jog on the spot.",
     A.run_in_place, {"freq": 2.0, "knee": 40.0}, 28, True, 26, ["fitness"]),
    ("high_knees", "High Knees", "sport", "Knees driven up high.",
     A.high_knees, {}, 20, True, 26, ["fitness"]),
    ("high_knees_fast", "Knee Drive", "sport", "High knees at full speed.",
     A.high_knees, {"freq": 5.0}, 14, True, 26, ["fitness"]),
    ("punch_combo", "Punch Combo", "sport", "A two-punch combination.",
     A.punch_combo, {}, 26, True, 26, ["fight"]),
    ("punch_fast", "Rapid Jabs", "sport", "Fast repeated jabs.",
     A.punch_combo, {"freq": 5.0}, 16, True, 26, ["fight"]),
    ("punch_slow", "Heavy Punches", "sport", "Slow, heavy blows.",
     A.punch_combo, {"freq": 1.2}, 44, True, 26, ["fight"]),
    ("kick_high", "High Kick", "sport", "A single high kick.",
     A.kick_high, {}, 30, False, 22, ["fight"]),
    ("kick_high_left", "Left High Kick", "sport", "A high kick from the other leg.",
     A.kick_high, {"side": "left"}, 30, False, 22, ["fight"]),
    ("kick_high_loop", "Kick Drill", "sport", "High kicks on repeat.",
     A.kick_high, {}, 30, True, 24, ["fight"]),
    ("cartwheel", "Cartwheel", "sport", "A sideways cartwheel.",
     A.cartwheel, {}, 30, False, 28, ["acrobat"]),
    ("cartwheel_loop", "Cartwheel Run", "sport", "Cartwheels one after another.",
     A.cartwheel, {}, 30, True, 28, ["acrobat"]),
    ("backflip", "Backflip", "sport", "A full backflip.",
     A.backflip, {}, 26, False, 28, ["acrobat"]),
    ("backflip_loop", "Backflip Chain", "sport", "Backflips without pause.",
     A.backflip, {}, 26, True, 28, ["acrobat"]),
    ("handstand", "Handstand", "sport", "Upside down, wobbling slightly.",
     A.handstand, {}, 80, True, 26, ["acrobat"]),
    ("handstand_steady", "Steady Handstand", "sport", "A handstand with almost no wobble.",
     A.handstand, {"wobble": 1.5, "freq": 0.3}, 110, True, 26, ["acrobat"]),
    ("handstand_wobble", "Wobbly Handstand", "sport", "A handstand that is barely holding.",
     A.handstand, {"wobble": 14.0, "freq": 1.6}, 40, True, 28, ["acrobat"]),
    ("zombie", "Zombie Walk", "sport", "Stiff arms, dragging feet.",
     A.zombie_walk, {}, 40, True, 26, ["spooky"]),
    ("zombie_fast", "Zombie Rush", "sport", "A zombie in a hurry.",
     A.zombie_walk, {"freq": 2.4}, 22, True, 26, ["spooky"]),
    ("crawl", "Crawl", "sport", "Crawling forward on all fours.",
     A.crawl, {}, 40, True, 28, ["floor"]),
    ("crawl_fast", "Scramble", "sport", "A frantic crawl.",
     A.crawl, {"freq": 2.5}, 22, True, 28, ["floor"]),
    ("golf_swing", "Golf Swing", "sport", "A full golf swing and follow through.",
     A.golf_swing, {}, 44, False, 26, ["game"]),
    ("golf_swing_loop", "Driving Range", "sport", "Golf swings on repeat.",
     A.golf_swing, {}, 44, True, 26, ["game"]),
    ("basketball", "Jump Shot", "sport", "A basketball jump shot.",
     A.basketball_shot, {}, 34, False, 26, ["game"]),
    ("basketball_loop", "Shooting Practice", "sport", "Jump shots, over and over.",
     A.basketball_shot, {}, 34, True, 26, ["game"]),

    # ---------------------------------------------------------------- roleplay
    ("cast_channel", "Channel Spell", "magic", "Both hands raised, channelling.",
     A.cast_spell, {}, 40, True, 26, ["spell"]),
    ("cast_channel_fast", "Rapid Cast", "magic", "A faster channel.",
     A.cast_spell, {"freq": 3.0}, 22, True, 26, ["spell"]),
    ("cast_burst", "Spell Burst", "magic", "A gathered, released blast.",
     A.cast_spell, {"style": "burst"}, 34, False, 26, ["spell"]),
    ("cast_burst_loop", "Barrage", "magic", "Spell bursts on repeat.",
     A.cast_spell, {"style": "burst"}, 34, True, 26, ["spell"]),
    ("cast_summon", "Summon", "magic", "Slow circular gestures while rising.",
     A.cast_spell, {"style": "summon"}, 70, True, 30, ["spell"]),
    ("cast_summon_slow", "Great Summoning", "magic", "A long, deliberate summoning.",
     A.cast_spell, {"style": "summon", "freq": 0.5}, 120, True, 32, ["spell"]),
    ("archer_draw", "Draw Bow", "magic", "Drawing and holding an imaginary bow.",
     A.archer, {}, 44, True, 24, ["combat"]),
    ("archer_ready", "Bow Ready", "magic", "Bow held ready, arm steady.",
     A.archer, {"draw": False}, 70, True, 20, ["combat"]),
    ("sword_flourish", "Sword Flourish", "magic", "A showy blade flourish.",
     A.sword_flourish, {}, 40, True, 28, ["combat"]),
    ("sword_flourish_fast", "Blade Dance", "magic", "Flourishes chained together.",
     A.sword_flourish, {"freq": 2.5}, 26, True, 28, ["combat"]),
    ("sword_salute", "Blade Salute", "magic", "A single formal blade salute.",
     A.sword_flourish, {}, 40, False, 26, ["combat"]),
    ("shield_brace", "Shield Brace", "magic", "Braced behind a shield.",
     A.shield_brace, {}, 60, True, 22, ["combat"]),
    ("shield_hold", "Hold the Line", "magic", "A rock-steady shield stance.",
     A.shield_brace, {"freq": 0.2}, 100, True, 22, ["combat"]),
    ("fishing", "Fishing", "magic", "Holding a rod, waiting patiently.",
     A.fishing, {}, 90, True, 22, ["idle"]),
    ("fishing_bite", "Got a Bite", "magic", "The rod twitching.",
     A.fishing, {"freq": 2.0}, 34, True, 24, ["idle"]),
    ("toast", "Toast", "magic", "Raising a drink.",
     A.toast, {}, 40, False, 22, ["social"]),
    ("toast_loop", "Cheers", "magic", "Toasting, repeatedly.",
     A.toast, {}, 40, True, 22, ["social"]),
    ("photo", "Take a Photo", "magic", "Framing a shot with both hands.",
     A.camera_photo, {}, 60, True, 20, ["social"]),
    ("photo_selfie", "Selfie", "magic", "One arm out, definitely a selfie.",
     A.camera_photo, {"freq": 1.2}, 40, True, 22, ["social"]),
    ("dust_off", "Dust Off Shoulder", "magic", "Brushing it off, unbothered.",
     A.dust_off_shoulder, {}, 34, True, 24, ["taunt"]),
    ("dust_off_slow", "Unbothered", "magic", "A slower, smugger brush-off.",
     A.dust_off_shoulder, {"freq": 0.6}, 56, True, 24, ["taunt"]),
]


def check_loop_closes(emote_id, part, frames):
    """A looping emote must end where it started, or it visibly jolts once per cycle.

    A rotation channel that differs by a whole turn is fine — that is a spin, and 360 degrees is
    the same orientation as zero.
    """
    first, last = frames[0], frames[-1]
    for key, default in (("rot", 0.0), ("pos", 0.0), ("scale", 1.0)):
        a = first.get(key, [default] * 3)
        b = last.get(key, [default] * 3)
        for axis in range(3):
            delta = abs(b[axis] - a[axis])
            if key == "rot" and abs(delta % 360.0) < 0.5:
                continue
            if delta > 0.5:
                raise ValueError(
                    "emote '%s' does not loop cleanly: %s.%s[%d] goes %.2f -> %.2f"
                    % (emote_id, part, key, axis, a[axis], b[axis]))


def build(entry):
    emote_id, name, category, description, archetype, kwargs, length, loop, samples, tags = entry
    tracks_spec = archetype(**kwargs)
    tracks = {}
    for part, channels in tracks_spec.items():
        frames = sample_track(channels, samples, length, loop)
        # Drop tracks that ended up doing nothing at all.
        if any(("rot" in f or "pos" in f or "scale" in f) for f in frames):
            if loop:
                check_loop_closes(emote_id, part, frames)
            tracks[part] = frames
    if not tracks:
        raise ValueError("emote '%s' produced no animated tracks" % emote_id)

    return {
        "format": 1,
        "id": emote_id,
        "name": name,
        "author": AUTHOR,
        "description": description,
        "category": category,
        "tags": tags,
        "length": length,
        "loop": loop,
        "blendIn": 3 if length < 40 else 5,
        "blendOut": 3 if length < 40 else 5,
        "tracks": tracks,
    }


def main():
    out = os.path.abspath(OUT_DIR)
    os.makedirs(out, exist_ok=True)

    # Start from a clean slate so removed entries do not linger in the jar.
    for existing in os.listdir(out):
        if existing.endswith(".emote.json") or existing == "index.json":
            os.remove(os.path.join(out, existing))

    seen = set()
    written = []
    total_keyframes = 0
    for entry in CATALOGUE:
        emote_id = entry[0]
        if emote_id in seen:
            raise ValueError("duplicate emote id: " + emote_id)
        seen.add(emote_id)

        emote = build(entry)
        total_keyframes += sum(len(frames) for frames in emote["tracks"].values())
        path = os.path.join(out, emote_id + ".emote.json")
        with open(path, "w", encoding="utf-8") as handle:
            # Compact: these ship inside the jar and are read by the mod, not edited by hand.
            json.dump(emote, handle, separators=(",", ":"), ensure_ascii=False)
            handle.write("\n")
        written.append(emote_id)

    with open(os.path.join(out, "index.json"), "w", encoding="utf-8") as handle:
        json.dump({"format": 1, "count": len(written), "emotes": sorted(written)},
                  handle, indent=1, ensure_ascii=False)
        handle.write("\n")

    print("Wrote %d emotes, %d keyframes total, into %s" % (len(written), total_keyframes, out))
    categories = {}
    for entry in CATALOGUE:
        categories[entry[2]] = categories.get(entry[2], 0) + 1
    for category in sorted(categories):
        print("  %-10s %3d" % (category, categories[category]))


if __name__ == "__main__":
    main()
