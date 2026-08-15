"""Reusable emote choreographies.

Each function returns ``{part: {channel: curve}}``. Parameters exist so the same choreography can
produce genuinely different emotes — a slow, wide wave and a frantic little one are the same shape
with different numbers, not two copies of the same file.

Model conventions (see EmotePoseApplier for the matching Java side):
  * limbs hang downwards, so **negative rx swings an arm or leg forwards and up**
    (-90 = straight ahead, -180 = straight up)
  * **rz spreads a limb sideways**: positive moves the right limb outwards, negative the left one
  * head and torso point upwards, so **positive rx tips them forwards**
  * on the root, **+Y is up and -Z is the way the player faces**
"""

from emote_dsl import (add, bounce, const, cosine, hold_then, pulse, ramp, sawtooth, seq, shifted,
                       sine, steps, triangle, turn)

# ---------------------------------------------------------------------------
# Greetings and social
# ---------------------------------------------------------------------------


def wave(side="right", raise_angle=-150.0, swing=28.0, freq=3.0, lean=4.0):
    """Arm held up, hand swinging side to side."""
    sign = 1.0 if side == "right" else -1.0
    arm = "rightArm" if side == "right" else "leftArm"
    other = "leftArm" if side == "right" else "rightArm"
    return {
        arm: {"rx": const(raise_angle), "rz": sine(swing, freq, 0.0, 18.0 * sign)},
        other: {"rz": const(-6.0 * sign), "rx": sine(4.0, freq, 0.5)},
        "head": {"ry": sine(6.0, freq, 0.25), "rx": const(-4.0)},
        "body": {"rz": sine(lean, freq, 0.0)},
    }


def big_wave(side="right", freq=2.0):
    """Both-arm overhead wave, the "over here!" gesture."""
    return {
        "rightArm": {"rx": const(-165.0), "rz": sine(24.0, freq, 0.0, 14.0)},
        "leftArm": {"rx": const(-165.0), "rz": sine(24.0, freq, 0.5, -14.0)},
        "head": {"rx": const(-12.0), "ry": sine(8.0, freq, 0.25)},
        "body": {"rz": sine(5.0, freq)},
        "root": {"py": bounce(1.2, freq)},
    }


def salute(side="right", hold=0.45):
    sign = 1.0 if side == "right" else -1.0
    arm = "rightArm" if side == "right" else "leftArm"
    return {
        arm: {
            "rx": seq([(0.0, 0.0), (0.2, -140.0), (hold, -148.0), (0.85, -140.0), (0.95, 0.0)]),
            "rz": seq([(0.0, 0.0), (0.2, 34.0 * sign), (0.85, 34.0 * sign), (0.95, 0.0)]),
            "ry": const(-20.0 * sign),
        },
        "head": {"rx": seq([(0.0, 0.0), (0.2, -8.0), (0.85, -8.0), (1.0, 0.0)])},
        "body": {"rx": seq([(0.0, 0.0), (0.2, -3.0), (0.85, -3.0), (1.0, 0.0)])},
    }


def bow(depth=52.0, hold=0.35, arm_style="side"):
    body = seq([(0.0, 0.0), (0.25, depth), (0.25 + hold, depth), (0.85, 0.0)])
    arms = {}
    if arm_style == "cross":
        arms = {
            "rightArm": {"rx": seq([(0.0, 0.0), (0.25, -62.0), (0.85, 0.0)]),
                         "rz": seq([(0.0, 0.0), (0.25, -34.0), (0.85, 0.0)])},
            "leftArm": {"rx": seq([(0.0, 0.0), (0.25, -62.0), (0.85, 0.0)]),
                        "rz": seq([(0.0, 0.0), (0.25, 34.0), (0.85, 0.0)])},
        }
    elif arm_style == "sweep":
        arms = {
            "rightArm": {"rx": seq([(0.0, 0.0), (0.25, -95.0), (0.85, 0.0)]),
                         "rz": seq([(0.0, 0.0), (0.25, 52.0), (0.85, 0.0)])},
            "leftArm": {"rz": seq([(0.0, 0.0), (0.25, -18.0), (0.85, 0.0)])},
        }
    else:
        arms = {
            "rightArm": {"rx": seq([(0.0, 0.0), (0.25, 24.0), (0.85, 0.0)])},
            "leftArm": {"rx": seq([(0.0, 0.0), (0.25, 24.0), (0.85, 0.0)])},
        }
    tracks = {"body": {"rx": body},
              "head": {"rx": seq([(0.0, 0.0), (0.25, -18.0), (0.25 + hold, -18.0), (0.85, 0.0)])},
              "root": {"pz": scaled_seq(depth, 0.03)}}
    tracks.update(arms)
    return tracks


def scaled_seq(depth, factor):
    return seq([(0.0, 0.0), (0.25, depth * factor), (0.6, depth * factor), (0.85, 0.0)])


def handshake(freq=3.0):
    return {
        "rightArm": {"rx": add(const(-88.0), sine(12.0, freq)), "rz": const(-18.0)},
        "leftArm": {"rx": sine(6.0, freq, 0.5)},
        "body": {"rx": const(9.0), "ry": const(-8.0)},
        "head": {"rx": const(6.0), "ry": const(-10.0)},
    }


def point(side="right", pitch=-88.0, yaw=0.0, jab=6.0, freq=2.0):
    sign = 1.0 if side == "right" else -1.0
    arm = "rightArm" if side == "right" else "leftArm"
    return {
        arm: {"rx": add(const(pitch), sine(jab, freq)), "rz": const(10.0 * sign + yaw * sign)},
        "head": {"ry": const(yaw * 0.5 * sign), "rx": const(pitch * 0.06)},
        "body": {"ry": const(yaw * 0.25 * sign)},
    }


def nod(freq=2.0, amp=17.0, agree=True):
    if agree:
        return {"head": {"rx": sine(amp, freq, 0.0, 4.0)},
                "body": {"rx": sine(amp * 0.16, freq)}}
    return {"head": {"ry": sine(amp * 1.5, freq)},
            "body": {"ry": sine(amp * 0.2, freq)}}


def shrug(hold=0.4):
    lift = seq([(0.0, 0.0), (0.25, 1.6), (0.25 + hold, 1.6), (0.85, 0.0)])
    return {
        "rightArm": {"rx": seq([(0.0, 0.0), (0.25, -34.0), (0.25 + hold, -34.0), (0.85, 0.0)]),
                     "rz": seq([(0.0, 0.0), (0.25, 46.0), (0.25 + hold, 46.0), (0.85, 0.0)]),
                     "py": lift},
        "leftArm": {"rx": seq([(0.0, 0.0), (0.25, -34.0), (0.25 + hold, -34.0), (0.85, 0.0)]),
                    "rz": seq([(0.0, 0.0), (0.25, -46.0), (0.25 + hold, -46.0), (0.85, 0.0)]),
                    "py": lift},
        "head": {"rx": seq([(0.0, 0.0), (0.25, -10.0), (0.85, 0.0)]),
                 "rz": seq([(0.0, 0.0), (0.25, 7.0), (0.85, 0.0)])},
    }


def clap(freq=4.0, height=-58.0, spread=30.0):
    swing = sine(spread * 0.5, freq, 0.0, spread * 0.5)
    return {
        "rightArm": {"rx": const(height), "rz": add(const(4.0), swing), "ry": const(-38.0)},
        "leftArm": {"rx": const(height), "rz": add(const(-4.0), scaled_neg(swing)), "ry": const(38.0)},
        "head": {"rx": sine(4.0, freq, 0.25, -6.0)},
        "root": {"py": bounce(0.7, freq * 0.5)},
    }


def scaled_neg(fn):
    return lambda u: -fn(u)


def facepalm(hold=0.5):
    return {
        "rightArm": {"rx": seq([(0.0, 0.0), (0.22, -152.0), (0.22 + hold, -150.0), (0.9, 0.0)]),
                     "rz": seq([(0.0, 0.0), (0.22, 26.0), (0.9, 0.0)])},
        "head": {"rx": seq([(0.0, 0.0), (0.22, 26.0), (0.22 + hold, 28.0), (0.9, 0.0)])},
        "body": {"rx": seq([(0.0, 0.0), (0.3, 12.0), (0.9, 0.0)])},
    }


def applause_over_head(freq=3.0):
    return {
        "rightArm": {"rx": const(-158.0), "rz": sine(16.0, freq, 0.0, 12.0)},
        "leftArm": {"rx": const(-158.0), "rz": sine(16.0, freq, 0.5, -12.0)},
        "head": {"rx": const(-16.0)},
        "root": {"py": bounce(1.6, freq * 0.5)},
    }


# ---------------------------------------------------------------------------
# Dances
# ---------------------------------------------------------------------------


def dance_bounce(freq=2.0, arm=42.0, hip=9.0, height=2.4):
    return {
        "root": {"py": bounce(height, freq)},
        "body": {"rz": sine(hip, freq * 0.5), "ry": sine(hip * 0.7, freq * 0.5, 0.25)},
        "head": {"rx": sine(7.0, freq, 0.0, -3.0), "rz": sine(5.0, freq * 0.5, 0.5)},
        "rightArm": {"rx": sine(arm, freq, 0.0, -46.0), "rz": sine(12.0, freq * 0.5, 0.0, 16.0)},
        "leftArm": {"rx": sine(arm, freq, 0.5, -46.0), "rz": sine(12.0, freq * 0.5, 0.5, -16.0)},
        "rightLeg": {"rx": sine(11.0, freq, 0.5)},
        "leftLeg": {"rx": sine(11.0, freq, 0.0)},
    }


def dance_sway(freq=1.0, amp=15.0, arm_amp=34.0):
    return {
        "body": {"rz": sine(amp, freq), "ry": sine(amp * 0.5, freq, 0.25)},
        "head": {"rz": sine(amp * 0.6, freq, 0.08), "ry": sine(7.0, freq, 0.25)},
        "rightArm": {"rx": sine(arm_amp, freq, 0.0, -30.0), "rz": sine(20.0, freq, 0.0, 22.0)},
        "leftArm": {"rx": sine(arm_amp, freq, 0.5, -30.0), "rz": sine(20.0, freq, 0.5, -22.0)},
        "rightLeg": {"rz": sine(6.0, freq, 0.0, 3.0)},
        "leftLeg": {"rz": sine(6.0, freq, 0.5, -3.0)},
        "root": {"px": sine(1.6, freq), "py": bounce(0.8, freq * 2.0)},
    }


def dance_spin(turns=1.0, arm=-96.0, tilt=8.0, hop=1.4, freq=2.0):
    return {
        "root": {"ry": turn(360.0 * turns), "py": bounce(hop, freq)},
        "body": {"rz": sine(tilt, freq * 0.5)},
        "rightArm": {"rx": const(arm), "rz": const(58.0)},
        "leftArm": {"rx": const(arm), "rz": const(-58.0)},
        "head": {"rx": const(-8.0), "ry": sine(10.0, freq)},
        "rightLeg": {"rx": sine(9.0, freq)},
        "leftLeg": {"rx": sine(9.0, freq, 0.5)},
    }


def moonwalk(distance=9.0, freq=2.0, lean=11.0):
    """Feet slide backwards while the body leans forward — the illusion of walking on the spot."""
    return {
        "root": {"pz": sine(distance * 0.25, freq, 0.0, distance * 0.12), "py": bounce(0.6, freq * 2.0)},
        "body": {"rx": add(const(lean), sine(3.0, freq)), "rz": sine(4.0, freq * 0.5)},
        "head": {"rx": const(-lean * 0.8), "ry": sine(6.0, freq * 0.5)},
        "rightArm": {"rx": sine(26.0, freq, 0.0, -8.0), "rz": const(10.0)},
        "leftArm": {"rx": sine(26.0, freq, 0.5, -8.0), "rz": const(-10.0)},
        "rightLeg": {"rx": sine(24.0, freq, 0.0, -6.0), "py": bounce(1.1, freq, 0.0)},
        "leftLeg": {"rx": sine(24.0, freq, 0.5, -6.0), "py": bounce(1.1, freq, 0.5)},
    }


def floss(freq=2.0, amp=30.0, arm_swing=44.0):
    """Arms swing one way while the hips go the other."""
    return {
        "body": {"ry": sine(amp * 0.55, freq), "rz": sine(amp * 0.35, freq, 0.25)},
        "root": {"px": sine(1.3, freq, 0.25), "py": bounce(0.7, freq * 2.0)},
        "rightArm": {"rx": sine(arm_swing * 0.35, freq, 0.5, -14.0),
                     "rz": sine(arm_swing, freq, 0.5, 26.0)},
        "leftArm": {"rx": sine(arm_swing * 0.35, freq, 0.0, -14.0),
                    "rz": sine(arm_swing, freq, 0.0, -26.0)},
        "head": {"ry": sine(9.0, freq, 0.1)},
        "rightLeg": {"rz": sine(5.0, freq, 0.0, 3.0)},
        "leftLeg": {"rz": sine(5.0, freq, 0.5, -3.0)},
    }


def macarena(freq=1.0):
    """Eight-count arm choreography over a hip sway."""
    right_rx = seq([(0.0, -78.0), (0.13, -78.0), (0.26, -108.0), (0.39, -142.0),
                    (0.52, -150.0), (0.65, -120.0), (0.78, -60.0), (0.9, -20.0)])
    left_rx = shifted(right_rx, -0.06)
    return {
        "rightArm": {"rx": right_rx, "rz": seq([(0.0, -18.0), (0.3, 8.0), (0.6, 30.0), (0.9, 4.0)])},
        "leftArm": {"rx": left_rx, "rz": seq([(0.0, 18.0), (0.3, -8.0), (0.6, -30.0), (0.9, -4.0)])},
        "body": {"ry": sine(13.0, freq * 2.0), "rz": sine(8.0, freq * 2.0, 0.25)},
        "root": {"py": bounce(1.1, freq * 4.0), "px": sine(1.0, freq * 2.0)},
        "head": {"ry": sine(10.0, freq * 2.0, 0.15)},
    }


def disco(freq=2.0, reach=-150.0):
    """Alternating point to the sky and down to the hip."""
    return {
        "rightArm": {"rx": seq([(0.0, reach), (0.5, -18.0), (0.99, reach)]),
                     "rz": seq([(0.0, 34.0), (0.5, 12.0), (0.99, 34.0)])},
        "leftArm": {"rx": seq([(0.0, -18.0), (0.5, reach), (0.99, -18.0)]),
                    "rz": seq([(0.0, -12.0), (0.5, -34.0), (0.99, -12.0)])},
        "body": {"rz": sine(11.0, freq * 0.5), "ry": sine(9.0, freq * 0.5, 0.25)},
        "root": {"py": bounce(1.7, freq)},
        "head": {"rx": sine(9.0, freq, 0.0, -8.0)},
        "rightLeg": {"rx": sine(10.0, freq, 0.5)},
        "leftLeg": {"rx": sine(10.0, freq, 0.0)},
    }


def robot(steps_count=8, amp=70.0):
    """Stepped, snapping motion — no smoothing anywhere."""
    right = steps([(i / steps_count, -amp if i % 2 else -12.0) for i in range(steps_count)])
    left = steps([(i / steps_count, -12.0 if i % 2 else -amp) for i in range(steps_count)])
    return {
        "rightArm": {"rx": right, "rz": steps([(i / steps_count, 22.0 if i % 3 else 6.0)
                                               for i in range(steps_count)])},
        "leftArm": {"rx": left, "rz": steps([(i / steps_count, -6.0 if i % 3 else -22.0)
                                             for i in range(steps_count)])},
        "head": {"ry": steps([(i / steps_count, 24.0 if i % 4 < 2 else -24.0) for i in range(steps_count)])},
        "body": {"ry": steps([(i / steps_count, 9.0 if i % 4 < 2 else -9.0) for i in range(steps_count)])},
        "root": {"py": steps([(i / steps_count, 0.9 if i % 2 else 0.0) for i in range(steps_count)])},
    }


def breakdance_spin(turns=2.0, freq=2.0):
    return {
        "root": {"ry": turn(360.0 * turns),
                 "rx": const(-72.0), "py": const(-5.0), "pz": const(2.0)},
        "rightArm": {"rx": const(-142.0), "rz": const(24.0)},
        "leftArm": {"rx": const(-28.0), "rz": const(-58.0)},
        "rightLeg": {"rx": sine(44.0, freq, 0.0, -52.0), "rz": const(28.0)},
        "leftLeg": {"rx": sine(44.0, freq, 0.5, -52.0), "rz": const(-28.0)},
        "body": {"rz": sine(12.0, freq)},
        "head": {"rx": const(22.0)},
    }


def worm(freq=1.0, amplitude=3.4):
    """Body wave travelling head to toe, close to the floor."""
    return {
        "root": {"rx": const(-76.0), "py": add(const(-6.0), bounce(amplitude, freq)), "pz": const(3.0)},
        "body": {"rx": sine(20.0, freq, 0.0, 4.0), "py": sine(1.4, freq)},
        "head": {"rx": sine(22.0, freq, 0.12, -12.0)},
        "rightArm": {"rx": const(-158.0), "rz": const(14.0)},
        "leftArm": {"rx": const(-158.0), "rz": const(-14.0)},
        "rightLeg": {"rx": sine(20.0, freq, 0.55, 8.0)},
        "leftLeg": {"rx": sine(20.0, freq, 0.6, 8.0)},
    }


def hip_shake(freq=4.0, amp=17.0):
    return {
        "body": {"ry": sine(amp, freq), "rz": sine(amp * 0.35, freq * 2.0)},
        "root": {"px": sine(1.0, freq), "py": bounce(0.6, freq)},
        "rightArm": {"rx": const(-32.0), "rz": add(const(30.0), sine(8.0, freq))},
        "leftArm": {"rx": const(-32.0), "rz": add(const(-30.0), sine(8.0, freq, 0.5))},
        "rightLeg": {"rz": sine(5.0, freq, 0.0, 4.0)},
        "leftLeg": {"rz": sine(5.0, freq, 0.5, -4.0)},
        "head": {"ry": sine(6.0, freq, 0.5)},
    }


def air_guitar(freq=3.0):
    return {
        "rightArm": {"rx": add(const(-56.0), sine(46.0, freq)), "rz": const(28.0), "ry": const(-24.0)},
        "leftArm": {"rx": add(const(-92.0), sine(10.0, freq * 0.5)), "rz": const(-46.0)},
        "body": {"rx": add(const(9.0), sine(6.0, freq * 0.5)), "ry": const(-12.0)},
        "head": {"rx": sine(14.0, freq * 0.5, 0.0, 6.0), "ry": const(-10.0)},
        "root": {"py": bounce(1.4, freq * 0.5)},
        "rightLeg": {"rx": sine(10.0, freq * 0.5)},
        "leftLeg": {"rx": const(-8.0)},
    }


def drumming(freq=4.0):
    return {
        "rightArm": {"rx": add(const(-64.0), sine(30.0, freq)), "rz": const(20.0)},
        "leftArm": {"rx": add(const(-64.0), sine(30.0, freq, 0.5)), "rz": const(-20.0)},
        "head": {"rx": sine(11.0, freq * 0.5, 0.0, 6.0)},
        "body": {"rx": const(8.0), "ry": sine(6.0, freq * 0.5)},
        "root": {"py": bounce(0.8, freq * 0.5)},
        "rightLeg": {"rx": sine(14.0, freq * 0.5, 0.0, -4.0)},
    }


def dj_scratch(freq=3.0):
    return {
        "rightArm": {"rx": add(const(-74.0), sine(24.0, freq)), "rz": const(26.0), "ry": const(-30.0)},
        "leftArm": {"rx": const(-146.0), "rz": const(-22.0)},
        "head": {"rx": sine(10.0, freq * 0.5, 0.0, 8.0), "ry": const(-16.0)},
        "body": {"ry": const(-14.0), "rz": sine(6.0, freq * 0.5)},
        "root": {"py": bounce(1.1, freq * 0.5)},
    }


def dab(side="right"):
    sign = 1.0 if side == "right" else -1.0
    up_arm = "rightArm" if side == "right" else "leftArm"
    across = "leftArm" if side == "right" else "rightArm"
    return {
        up_arm: {"rx": seq([(0.0, 0.0), (0.18, -148.0), (0.8, -146.0), (0.95, 0.0)]),
                 "rz": seq([(0.0, 0.0), (0.18, 42.0 * sign), (0.8, 42.0 * sign), (0.95, 0.0)])},
        across: {"rx": seq([(0.0, 0.0), (0.18, -128.0), (0.8, -126.0), (0.95, 0.0)]),
                 "rz": seq([(0.0, 0.0), (0.18, 34.0 * sign), (0.8, 34.0 * sign), (0.95, 0.0)])},
        "head": {"rx": seq([(0.0, 0.0), (0.18, 34.0), (0.8, 34.0), (0.95, 0.0)]),
                 "ry": seq([(0.0, 0.0), (0.18, -22.0 * sign), (0.8, -22.0 * sign), (0.95, 0.0)])},
        "body": {"rz": seq([(0.0, 0.0), (0.18, -9.0 * sign), (0.8, -9.0 * sign), (0.95, 0.0)])},
    }


def kick_dance(freq=1.0, kick=-64.0):
    return {
        "rightLeg": {"rx": seq([(0.0, 0.0), (0.25, kick), (0.5, 0.0), (0.75, 0.0)])},
        "leftLeg": {"rx": seq([(0.0, 0.0), (0.25, 0.0), (0.5, 0.0), (0.75, kick)])},
        "rightArm": {"rx": sine(30.0, freq * 2.0, 0.0, -26.0), "rz": const(24.0)},
        "leftArm": {"rx": sine(30.0, freq * 2.0, 0.5, -26.0), "rz": const(-24.0)},
        "body": {"rz": sine(9.0, freq * 2.0), "rx": const(-4.0)},
        "root": {"py": bounce(1.2, freq * 2.0)},
        "head": {"ry": sine(8.0, freq * 2.0, 0.25)},
    }


def shuffle(freq=3.0):
    return {
        "rightLeg": {"rx": sine(30.0, freq, 0.0, -6.0), "py": bounce(1.6, freq)},
        "leftLeg": {"rx": sine(30.0, freq, 0.5, -6.0), "py": bounce(1.6, freq, 0.5)},
        "rightArm": {"rx": sine(22.0, freq, 0.5, -18.0), "rz": const(18.0)},
        "leftArm": {"rx": sine(22.0, freq, 0.0, -18.0), "rz": const(-18.0)},
        "body": {"rz": sine(6.0, freq * 0.5), "rx": const(5.0)},
        "root": {"py": bounce(0.9, freq)},
        "head": {"rx": const(-4.0), "ry": sine(6.0, freq * 0.5)},
    }


# ---------------------------------------------------------------------------
# Floating, flying and otherworldly
# ---------------------------------------------------------------------------


def float_idle(height=7.0, drift=2.2, freq=0.5, arm_out=52.0):
    """Hovering, arms drifting as if underwater."""
    return {
        "root": {"py": add(const(height), sine(drift, freq)), "ry": sine(7.0, freq * 0.5),
                 "rz": sine(3.0, freq * 0.5, 0.25)},
        "body": {"rx": sine(4.0, freq, 0.25, -6.0)},
        "head": {"rx": add(const(-12.0), sine(5.0, freq, 0.15))},
        "rightArm": {"rx": add(const(-26.0), sine(9.0, freq, 0.1)), "rz": add(const(arm_out), sine(7.0, freq))},
        "leftArm": {"rx": add(const(-26.0), sine(9.0, freq, 0.6)), "rz": add(const(-arm_out), sine(7.0, freq, 0.5))},
        "rightLeg": {"rx": add(const(-16.0), sine(6.0, freq, 0.3)), "rz": const(7.0)},
        "leftLeg": {"rx": add(const(-10.0), sine(6.0, freq, 0.8)), "rz": const(-7.0)},
    }


def heaven(height=11.0, freq=0.4, rise=True):
    """Rising towards the sky, arms open, head tilted back.

    With ``rise`` the player climbs across the emote, which only makes sense played once. A looping
    variant holds the altitude instead and just breathes.
    """
    climb = add(ramp(0.0, height), sine(1.1, freq)) if rise else add(const(height), sine(1.4, freq))
    return {
        "root": {"py": climb, "ry": sine(5.0, freq * 0.5)},
        "body": {"rx": const(-13.0)},
        "head": {"rx": add(const(-34.0), sine(3.0, freq))},
        "rightArm": {"rx": add(const(-166.0), sine(5.0, freq)), "rz": add(const(24.0), sine(4.0, freq))},
        "leftArm": {"rx": add(const(-166.0), sine(5.0, freq, 0.5)), "rz": add(const(-24.0), sine(4.0, freq, 0.5))},
        "rightLeg": {"rx": const(-8.0), "rz": const(5.0)},
        "leftLeg": {"rx": const(-4.0), "rz": const(-5.0)},
    }


def ascend(height=16.0, spin=1.0, freq=0.5):
    return {
        "root": {"py": ramp(0.0, height), "ry": ramp(0.0, 360.0 * spin), "rx": const(-6.0)},
        "body": {"rx": const(-8.0)},
        "head": {"rx": const(-26.0)},
        "rightArm": {"rx": const(-172.0), "rz": const(10.0)},
        "leftArm": {"rx": const(-172.0), "rz": const(-10.0)},
        "rightLeg": {"rx": const(-10.0)},
        "leftLeg": {"rx": const(-6.0)},
    }


def superhero_fly(freq=0.6, height=9.0):
    return {
        "root": {"rx": const(-74.0), "py": add(const(height), sine(1.6, freq)), "pz": const(2.0)},
        "rightArm": {"rx": add(const(-172.0), sine(5.0, freq)), "rz": const(8.0)},
        "leftArm": {"rx": const(-14.0), "rz": const(-16.0)},
        "head": {"rx": const(38.0)},
        "body": {"rz": sine(5.0, freq, 0.25)},
        "rightLeg": {"rx": add(const(6.0), sine(5.0, freq)), "rz": const(5.0)},
        "leftLeg": {"rx": add(const(4.0), sine(5.0, freq, 0.5)), "rz": const(-5.0)},
    }


def swim_air(freq=1.0):
    return {
        "root": {"rx": const(-78.0), "py": add(const(6.0), sine(1.4, freq * 2.0)), "pz": const(2.0)},
        "rightArm": {"rx": turn(-360.0), "rz": const(14.0)},
        "leftArm": {"rx": shifted(turn(-360.0), 0.5), "rz": const(-14.0)},
        "rightLeg": {"rx": sine(22.0, freq * 2.0, 0.0, 6.0)},
        "leftLeg": {"rx": sine(22.0, freq * 2.0, 0.5, 6.0)},
        "head": {"rx": sine(8.0, freq * 2.0, 0.0, 26.0)},
        "body": {"rx": sine(5.0, freq * 2.0)},
    }


def levitate_meditate(height=6.0, freq=0.35):
    return {
        "root": {"py": add(const(height), sine(1.3, freq)), "ry": sine(12.0, freq * 0.4)},
        "rightLeg": {"rx": const(-84.0), "rz": const(44.0), "py": const(-1.0)},
        "leftLeg": {"rx": const(-84.0), "rz": const(-44.0), "py": const(-1.0)},
        "rightArm": {"rx": const(-46.0), "rz": const(40.0)},
        "leftArm": {"rx": const(-46.0), "rz": const(-40.0)},
        "head": {"rx": add(const(6.0), sine(2.0, freq))},
        "body": {"rx": sine(2.0, freq, 0.3, -2.0)},
    }


def ghost_drift(height=5.0, freq=0.4):
    return {
        "root": {"py": add(const(height), sine(2.4, freq)), "ry": sine(22.0, freq * 0.5),
                 "rz": sine(6.0, freq * 0.7)},
        "body": {"rz": sine(7.0, freq, 0.2)},
        "head": {"rz": sine(9.0, freq, 0.3), "rx": const(-6.0)},
        "rightArm": {"rx": add(const(-96.0), sine(11.0, freq)), "rz": const(12.0)},
        "leftArm": {"rx": add(const(-96.0), sine(11.0, freq, 0.5)), "rz": const(-12.0)},
        "rightLeg": {"rx": add(const(-6.0), sine(8.0, freq, 0.25)), "rz": const(6.0)},
        "leftLeg": {"rx": add(const(-6.0), sine(8.0, freq, 0.75)), "rz": const(-6.0)},
    }


def moon_jump(height=13.0, freq=0.5):
    return {
        "root": {"py": bounce(height, freq), "rx": sine(6.0, freq)},
        "rightArm": {"rx": add(const(-104.0), sine(28.0, freq)), "rz": const(26.0)},
        "leftArm": {"rx": add(const(-104.0), sine(28.0, freq, 0.5)), "rz": const(-26.0)},
        "rightLeg": {"rx": sine(26.0, freq, 0.0, -14.0)},
        "leftLeg": {"rx": sine(26.0, freq, 0.5, -14.0)},
        "head": {"rx": const(-14.0)},
        "body": {"rx": sine(5.0, freq, 0.25, -4.0)},
    }


# ---------------------------------------------------------------------------
# Poses and idles
# ---------------------------------------------------------------------------


def sit(style="ground", breathe=1.6, freq=0.35):
    if style == "chair":
        return {
            "root": {"py": const(-7.0), "pz": const(1.0)},
            "rightLeg": {"rx": const(-88.0), "rz": const(6.0)},
            "leftLeg": {"rx": const(-88.0), "rz": const(-6.0)},
            "rightArm": {"rx": const(-16.0), "rz": const(9.0)},
            "leftArm": {"rx": const(-16.0), "rz": const(-9.0)},
            "body": {"rx": add(const(4.0), sine(breathe, freq))},
            "head": {"rx": add(const(-4.0), sine(1.6, freq, 0.2))},
        }
    if style == "cross":
        return {
            "root": {"py": const(-9.0)},
            "rightLeg": {"rx": const(-82.0), "rz": const(52.0)},
            "leftLeg": {"rx": const(-82.0), "rz": const(-52.0)},
            "rightArm": {"rx": const(-32.0), "rz": const(28.0)},
            "leftArm": {"rx": const(-32.0), "rz": const(-28.0)},
            "body": {"rx": add(const(6.0), sine(breathe, freq))},
            "head": {"rx": add(const(-2.0), sine(2.0, freq, 0.25)), "ry": sine(6.0, freq * 0.5)},
        }
    return {
        "root": {"py": const(-8.0), "pz": const(2.0)},
        "rightLeg": {"rx": const(-70.0), "rz": const(16.0)},
        "leftLeg": {"rx": const(-70.0), "rz": const(-16.0)},
        "rightArm": {"rx": const(28.0), "rz": const(22.0)},
        "leftArm": {"rx": const(28.0), "rz": const(-22.0)},
        "body": {"rx": add(const(-6.0), sine(breathe, freq))},
        "head": {"rx": add(const(4.0), sine(1.8, freq, 0.3))},
    }


def lie_down(face="up", freq=0.3):
    if face == "down":
        return {
            "root": {"rx": const(-88.0), "py": const(-11.0), "pz": const(4.0)},
            "rightArm": {"rx": const(-162.0), "rz": const(16.0)},
            "leftArm": {"rx": const(-162.0), "rz": const(-16.0)},
            "head": {"rx": const(28.0), "ry": sine(8.0, freq)},
            "body": {"rx": sine(1.6, freq)},
            "rightLeg": {"rx": const(4.0), "rz": const(7.0)},
            "leftLeg": {"rx": const(4.0), "rz": const(-7.0)},
        }
    return {
        "root": {"rx": const(88.0), "py": const(-11.0), "pz": const(-4.0)},
        "rightArm": {"rx": const(-18.0), "rz": const(34.0)},
        "leftArm": {"rx": const(-18.0), "rz": const(-34.0)},
        "head": {"rx": const(-16.0), "ry": sine(9.0, freq)},
        "body": {"rx": sine(1.8, freq, 0.2)},
        "rightLeg": {"rx": const(-6.0), "rz": const(9.0)},
        "leftLeg": {"rx": const(-6.0), "rz": const(-9.0)},
    }


def sleep(freq=0.25, snore=2.6):
    return {
        "root": {"rx": const(86.0), "py": const(-11.0), "pz": const(-4.0)},
        "body": {"rx": sine(snore, freq)},
        "head": {"rx": const(-22.0), "rz": const(16.0), "ry": sine(4.0, freq * 0.5)},
        "rightArm": {"rx": const(-38.0), "rz": const(38.0)},
        "leftArm": {"rx": const(-12.0), "rz": const(-26.0)},
        "rightLeg": {"rx": const(-14.0), "rz": const(12.0)},
        "leftLeg": {"rx": const(-4.0), "rz": const(-6.0)},
    }


def kneel(depth=1.0, pray=False):
    tracks = {
        "root": {"py": const(-6.0 * depth)},
        "rightLeg": {"rx": const(-88.0), "rz": const(9.0)},
        "leftLeg": {"rx": const(-16.0), "rz": const(-7.0)},
        "body": {"rx": const(9.0)},
        "head": {"rx": const(14.0)},
    }
    if pray:
        tracks["rightArm"] = {"rx": const(-118.0), "rz": const(-18.0)}
        tracks["leftArm"] = {"rx": const(-118.0), "rz": const(18.0)}
        tracks["head"] = {"rx": const(20.0)}
    else:
        tracks["rightArm"] = {"rx": const(-8.0), "rz": const(9.0)}
        tracks["leftArm"] = {"rx": const(-8.0), "rz": const(-9.0)}
    return tracks


def t_pose(spin=0.0, freq=0.0):
    tracks = {
        "rightArm": {"rz": const(90.0)},
        "leftArm": {"rz": const(-90.0)},
        "head": {"rx": const(0.0)},
    }
    if spin:
        tracks["root"] = {"ry": ramp(0.0, 360.0 * spin)}
    return tracks


def flex(side="both", freq=1.0, pump=9.0):
    right = {"rx": add(const(-96.0), sine(pump, freq)), "rz": const(62.0), "ry": const(-40.0)}
    left = {"rx": add(const(-96.0), sine(pump, freq, 0.5)), "rz": const(-62.0), "ry": const(40.0)}
    tracks = {"body": {"rx": const(-5.0), "ry": sine(5.0, freq)},
              "head": {"rx": const(-6.0), "ry": sine(9.0, freq, 0.25)},
              "rightLeg": {"rz": const(5.0)}, "leftLeg": {"rz": const(-5.0)}}
    if side in ("both", "right"):
        tracks["rightArm"] = right
    if side in ("both", "left"):
        tracks["leftArm"] = left
    if side == "right":
        tracks["leftArm"] = {"rz": const(-14.0)}
    if side == "left":
        tracks["rightArm"] = {"rz": const(14.0)}
    return tracks


def arms_crossed(freq=0.4, tap=0.0):
    tracks = {
        "rightArm": {"rx": const(-72.0), "rz": const(-42.0), "ry": const(-12.0)},
        "leftArm": {"rx": const(-72.0), "rz": const(42.0), "ry": const(12.0)},
        "body": {"rx": add(const(-3.0), sine(1.4, freq))},
        "head": {"rx": const(-4.0), "ry": sine(7.0, freq * 0.7)},
    }
    if tap:
        tracks["rightLeg"] = {"rx": sine(tap, 4.0, 0.0, -tap * 0.5)}
    return tracks


def hands_on_hips(freq=0.4):
    return {
        "rightArm": {"rx": const(-14.0), "rz": const(-46.0)},
        "leftArm": {"rx": const(-14.0), "rz": const(46.0)},
        "body": {"rx": add(const(-4.0), sine(1.5, freq)), "ry": sine(4.0, freq * 0.6)},
        "head": {"ry": sine(8.0, freq * 0.5), "rx": const(-5.0)},
        "rightLeg": {"rz": const(6.0)},
        "leftLeg": {"rz": const(-6.0)},
    }


def think(freq=0.6):
    return {
        "rightArm": {"rx": add(const(-124.0), sine(4.0, freq)), "rz": const(-22.0)},
        "leftArm": {"rx": const(-58.0), "rz": const(-38.0)},
        "head": {"rx": add(const(9.0), sine(4.0, freq * 0.5)), "ry": sine(11.0, freq * 0.4)},
        "body": {"rx": const(4.0), "ry": const(-6.0)},
    }


def stretch(freq=0.5, reach=-172.0):
    return {
        "rightArm": {"rx": seq([(0.0, 0.0), (0.3, reach), (0.6, reach), (0.95, 0.0)]),
                     "rz": seq([(0.0, 0.0), (0.3, 14.0), (0.95, 0.0)])},
        "leftArm": {"rx": seq([(0.0, 0.0), (0.3, reach), (0.6, reach), (0.95, 0.0)]),
                    "rz": seq([(0.0, 0.0), (0.3, -14.0), (0.95, 0.0)])},
        "body": {"rx": seq([(0.0, 0.0), (0.35, -14.0), (0.6, -14.0), (0.95, 0.0)])},
        "head": {"rx": seq([(0.0, 0.0), (0.35, -26.0), (0.6, -26.0), (0.95, 0.0)])},
        "root": {"py": seq([(0.0, 0.0), (0.35, 1.6), (0.6, 1.6), (0.95, 0.0)])},
    }


def yawn(freq=0.4):
    return {
        "head": {"rx": seq([(0.0, 0.0), (0.3, -30.0), (0.55, -30.0), (0.9, 6.0), (0.99, 0.0)])},
        "rightArm": {"rx": seq([(0.0, 0.0), (0.3, -156.0), (0.6, -150.0), (0.95, 0.0)]),
                     "rz": seq([(0.0, 0.0), (0.3, 22.0), (0.95, 0.0)])},
        "leftArm": {"rx": seq([(0.0, 0.0), (0.35, -60.0), (0.95, 0.0)]),
                    "rz": seq([(0.0, 0.0), (0.35, -22.0), (0.95, 0.0)])},
        "body": {"rx": seq([(0.0, 0.0), (0.35, -9.0), (0.9, 4.0), (0.99, 0.0)])},
        "root": {"py": seq([(0.0, 0.0), (0.35, 1.2), (0.9, -0.6), (0.99, 0.0)])},
    }


def breathe_idle(freq=0.3, depth=2.0):
    return {
        "body": {"rx": sine(depth, freq), "py": sine(0.4, freq)},
        "head": {"rx": sine(depth * 0.8, freq, 0.1), "ry": sine(5.0, freq * 0.4)},
        "rightArm": {"rx": sine(depth * 1.6, freq, 0.15), "rz": sine(1.6, freq, 0.0, 3.0)},
        "leftArm": {"rx": sine(depth * 1.6, freq, 0.65), "rz": sine(1.6, freq, 0.5, -3.0)},
        "root": {"py": sine(0.4, freq, 0.25)},
    }


# ---------------------------------------------------------------------------
# Emotion
# ---------------------------------------------------------------------------


def cry(freq=1.4):
    return {
        "rightArm": {"rx": add(const(-142.0), sine(7.0, freq)), "rz": const(20.0)},
        "leftArm": {"rx": add(const(-142.0), sine(7.0, freq, 0.5)), "rz": const(-20.0)},
        "head": {"rx": add(const(24.0), sine(5.0, freq)), "rz": sine(4.0, freq * 0.5)},
        "body": {"rx": add(const(14.0), sine(4.0, freq))},
        "root": {"py": sine(0.5, freq, 0.0, -0.6)},
        "rightLeg": {"rx": const(-4.0)},
    }


def laugh(freq=3.0):
    return {
        "head": {"rx": add(const(-24.0), sine(9.0, freq))},
        "body": {"rx": add(const(-10.0), sine(8.0, freq))},
        "rightArm": {"rx": add(const(-42.0), sine(16.0, freq)), "rz": const(24.0)},
        "leftArm": {"rx": add(const(-42.0), sine(16.0, freq, 0.5)), "rz": const(-24.0)},
        "root": {"py": bounce(0.9, freq)},
        "rightLeg": {"rx": sine(6.0, freq, 0.5)},
    }


def rage(freq=3.0):
    return {
        "rightArm": {"rx": add(const(-38.0), sine(20.0, freq)), "rz": const(36.0)},
        "leftArm": {"rx": add(const(-38.0), sine(20.0, freq, 0.5)), "rz": const(-36.0)},
        "head": {"rx": add(const(14.0), sine(6.0, freq * 2.0)), "ry": sine(9.0, freq)},
        "body": {"rx": add(const(11.0), sine(5.0, freq)), "ry": sine(7.0, freq)},
        "root": {"py": bounce(1.1, freq), "px": sine(0.5, freq * 2.0)},
        "rightLeg": {"rx": sine(24.0, freq, 0.0, -8.0)},
        "leftLeg": {"rx": sine(24.0, freq, 0.5, -8.0)},
    }


def cheer(freq=2.0, height=2.6):
    return {
        "rightArm": {"rx": sine(24.0, freq, 0.0, -156.0), "rz": const(22.0)},
        "leftArm": {"rx": sine(24.0, freq, 0.0, -156.0), "rz": const(-22.0)},
        "head": {"rx": const(-22.0)},
        "body": {"rx": const(-6.0)},
        "root": {"py": bounce(height, freq)},
        "rightLeg": {"rx": sine(16.0, freq, 0.0, -6.0)},
        "leftLeg": {"rx": sine(16.0, freq, 0.5, -6.0)},
    }


def defeat(freq=0.4):
    return {
        "head": {"rx": add(const(34.0), sine(3.0, freq))},
        "body": {"rx": add(const(22.0), sine(2.4, freq))},
        "rightArm": {"rx": add(const(16.0), sine(3.0, freq)), "rz": const(6.0)},
        "leftArm": {"rx": add(const(16.0), sine(3.0, freq, 0.5)), "rz": const(-6.0)},
        "root": {"py": add(const(-1.6), sine(0.4, freq))},
        "rightLeg": {"rx": const(-6.0)},
    }


def scared(freq=6.0):
    return {
        "rightArm": {"rx": const(-124.0), "rz": add(const(-30.0), sine(4.0, freq))},
        "leftArm": {"rx": const(-124.0), "rz": add(const(30.0), sine(4.0, freq, 0.5))},
        "head": {"rx": const(-9.0), "ry": sine(7.0, freq * 0.5), "rz": sine(3.0, freq)},
        "body": {"rx": const(9.0), "rz": sine(3.0, freq)},
        "root": {"px": sine(0.35, freq), "py": sine(0.25, freq * 2.0)},
        "rightLeg": {"rz": const(9.0)},
        "leftLeg": {"rz": const(-9.0)},
    }


def confused(freq=0.7):
    return {
        "rightArm": {"rx": const(-56.0), "rz": const(44.0)},
        "leftArm": {"rx": const(-56.0), "rz": const(-44.0)},
        "head": {"rz": sine(16.0, freq), "ry": sine(20.0, freq * 0.5), "rx": const(-6.0)},
        "body": {"ry": sine(8.0, freq * 0.5)},
    }


# ---------------------------------------------------------------------------
# Exercise and action
# ---------------------------------------------------------------------------


def jumping_jacks(freq=1.0):
    spread = sine(0.5, freq, 0.0, 0.5)
    return {
        "rightArm": {"rx": lambda u: -170.0 * spread(u), "rz": lambda u: 88.0 * (1.0 - spread(u)) * 0.4 + 12.0},
        "leftArm": {"rx": lambda u: -170.0 * spread(u), "rz": lambda u: -(88.0 * (1.0 - spread(u)) * 0.4 + 12.0)},
        "rightLeg": {"rz": lambda u: 24.0 * spread(u)},
        "leftLeg": {"rz": lambda u: -24.0 * spread(u)},
        "root": {"py": bounce(1.7, freq)},
        "head": {"rx": const(-4.0)},
        "body": {"rx": sine(3.0, freq)},
    }


def push_ups(freq=1.0):
    dip = sine(1.0, freq, 0.0, 0.0)
    return {
        "root": {"rx": const(-82.0), "py": lambda u: -8.0 + 2.6 * dip(u), "pz": const(3.0)},
        "rightArm": {"rx": const(-118.0), "rz": const(24.0)},
        "leftArm": {"rx": const(-118.0), "rz": const(-24.0)},
        "head": {"rx": lambda u: 22.0 - 8.0 * dip(u)},
        "body": {"rx": lambda u: 4.0 * dip(u)},
        "rightLeg": {"rx": const(6.0), "rz": const(6.0)},
        "leftLeg": {"rx": const(6.0), "rz": const(-6.0)},
    }


def sit_ups(freq=1.0):
    fold = sine(0.5, freq, 0.0, 0.5)
    return {
        "root": {"rx": const(84.0), "py": const(-10.0), "pz": const(-3.0)},
        "body": {"rx": lambda u: -54.0 * fold(u)},
        "head": {"rx": lambda u: 12.0 - 22.0 * fold(u)},
        "rightArm": {"rx": const(-142.0), "rz": const(18.0)},
        "leftArm": {"rx": const(-142.0), "rz": const(-18.0)},
        "rightLeg": {"rx": const(-58.0), "rz": const(10.0)},
        "leftLeg": {"rx": const(-58.0), "rz": const(-10.0)},
    }


def squats(freq=1.0, depth=6.0):
    down = sine(0.5, freq, 0.0, 0.5)
    return {
        "root": {"py": lambda u: -depth * down(u)},
        "rightLeg": {"rx": lambda u: -44.0 * down(u), "rz": const(9.0)},
        "leftLeg": {"rx": lambda u: -44.0 * down(u), "rz": const(-9.0)},
        "rightArm": {"rx": lambda u: -88.0 * down(u) - 8.0, "rz": const(10.0)},
        "leftArm": {"rx": lambda u: -88.0 * down(u) - 8.0, "rz": const(-10.0)},
        "body": {"rx": lambda u: 24.0 * down(u)},
        "head": {"rx": lambda u: -14.0 * down(u)},
    }


def run_in_place(freq=3.0, knee=58.0):
    return {
        "rightLeg": {"rx": sine(knee, freq, 0.0, -knee * 0.35), "py": bounce(2.2, freq)},
        "leftLeg": {"rx": sine(knee, freq, 0.5, -knee * 0.35), "py": bounce(2.2, freq, 0.5)},
        "rightArm": {"rx": sine(56.0, freq, 0.5, -34.0), "rz": const(14.0)},
        "leftArm": {"rx": sine(56.0, freq, 0.0, -34.0), "rz": const(-14.0)},
        "body": {"rx": const(11.0), "ry": sine(6.0, freq)},
        "head": {"rx": const(-9.0)},
        "root": {"py": bounce(1.2, freq * 2.0)},
    }


def high_knees(freq=3.0):
    return {
        "rightLeg": {"rx": sine(46.0, freq, 0.0, -50.0)},
        "leftLeg": {"rx": sine(46.0, freq, 0.5, -50.0)},
        "rightArm": {"rx": const(-88.0), "rz": const(16.0)},
        "leftArm": {"rx": const(-88.0), "rz": const(-16.0)},
        "root": {"py": bounce(1.5, freq)},
        "body": {"rx": const(5.0), "ry": sine(5.0, freq)},
    }


def punch_combo(freq=3.0):
    return {
        "rightArm": {"rx": seq([(0.0, -30.0), (0.15, -96.0), (0.3, -30.0), (0.99, -30.0)]),
                     "rz": const(-14.0)},
        "leftArm": {"rx": seq([(0.0, -30.0), (0.5, -30.0), (0.65, -96.0), (0.8, -30.0)]),
                    "rz": const(14.0)},
        "body": {"ry": seq([(0.0, 8.0), (0.15, -12.0), (0.5, 8.0), (0.65, 22.0), (0.9, 8.0)])},
        "head": {"ry": seq([(0.0, -6.0), (0.15, 8.0), (0.65, -12.0), (0.9, -6.0)])},
        "root": {"pz": seq([(0.0, 0.0), (0.15, -1.2), (0.3, 0.0), (0.65, -1.2), (0.8, 0.0)])},
        "rightLeg": {"rx": const(-8.0)},
        "leftLeg": {"rx": const(6.0)},
    }


def kick_high(side="right", freq=1.0):
    sign = 1.0 if side == "right" else -1.0
    leg = "rightLeg" if side == "right" else "leftLeg"
    other = "leftLeg" if side == "right" else "rightLeg"
    return {
        leg: {"rx": seq([(0.0, 0.0), (0.25, -132.0), (0.45, -120.0), (0.8, 0.0)])},
        other: {"rx": seq([(0.0, 0.0), (0.25, 12.0), (0.8, 0.0)])},
        "body": {"rx": seq([(0.0, 0.0), (0.25, -22.0), (0.8, 0.0)]),
                 "ry": seq([(0.0, 0.0), (0.25, 14.0 * sign), (0.8, 0.0)])},
        "rightArm": {"rx": seq([(0.0, 0.0), (0.25, -60.0), (0.8, 0.0)]), "rz": const(26.0)},
        "leftArm": {"rx": seq([(0.0, 0.0), (0.25, -60.0), (0.8, 0.0)]), "rz": const(-26.0)},
        "head": {"rx": seq([(0.0, 0.0), (0.25, 12.0), (0.8, 0.0)])},
        "root": {"py": seq([(0.0, 0.0), (0.25, 1.4), (0.8, 0.0)])},
    }


def cartwheel(freq=1.0):
    return {
        "root": {"rz": turn(360.0), "py": bounce(6.0, 1.0), "px": sine(2.0, 1.0)},
        "rightArm": {"rz": const(96.0), "rx": const(-8.0)},
        "leftArm": {"rz": const(-96.0), "rx": const(-8.0)},
        "rightLeg": {"rz": const(28.0)},
        "leftLeg": {"rz": const(-28.0)},
        "head": {"rx": const(-8.0)},
    }


def backflip(freq=1.0):
    return {
        "root": {"rx": turn(-360.0), "py": bounce(11.0, 1.0)},
        "body": {"rx": seq([(0.0, 0.0), (0.3, 26.0), (0.7, 26.0), (0.99, 0.0)])},
        "rightArm": {"rx": seq([(0.0, 0.0), (0.2, -168.0), (0.7, -120.0), (0.99, 0.0)]), "rz": const(12.0)},
        "leftArm": {"rx": seq([(0.0, 0.0), (0.2, -168.0), (0.7, -120.0), (0.99, 0.0)]), "rz": const(-12.0)},
        "rightLeg": {"rx": seq([(0.0, 0.0), (0.35, -108.0), (0.75, -20.0), (0.99, 0.0)])},
        "leftLeg": {"rx": seq([(0.0, 0.0), (0.35, -108.0), (0.75, -20.0), (0.99, 0.0)])},
        "head": {"rx": const(-12.0)},
    }


def handstand(freq=0.6, wobble=5.0):
    return {
        "root": {"rx": const(180.0), "py": const(-2.0), "rz": sine(wobble, freq)},
        "rightArm": {"rx": add(const(-8.0), sine(3.0, freq)), "rz": const(10.0)},
        "leftArm": {"rx": add(const(-8.0), sine(3.0, freq, 0.5)), "rz": const(-10.0)},
        "rightLeg": {"rx": add(const(6.0), sine(7.0, freq)), "rz": const(12.0)},
        "leftLeg": {"rx": add(const(6.0), sine(7.0, freq, 0.5)), "rz": const(-12.0)},
        "head": {"rx": const(-26.0)},
        "body": {"rz": sine(3.0, freq, 0.25)},
    }


def zombie_walk(freq=0.8):
    return {
        "rightArm": {"rx": add(const(-92.0), sine(6.0, freq)), "rz": const(6.0)},
        "leftArm": {"rx": add(const(-92.0), sine(6.0, freq, 0.5)), "rz": const(-6.0)},
        "rightLeg": {"rx": sine(26.0, freq, 0.0, -6.0)},
        "leftLeg": {"rx": sine(26.0, freq, 0.5, -6.0)},
        "head": {"rz": const(18.0), "rx": const(9.0), "ry": sine(9.0, freq * 0.5)},
        "body": {"rx": const(9.0), "rz": sine(6.0, freq * 0.5)},
        "root": {"py": bounce(0.8, freq)},
    }


def crawl(freq=1.0):
    return {
        "root": {"rx": const(-80.0), "py": const(-9.0), "pz": const(3.0)},
        "rightArm": {"rx": sine(34.0, freq, 0.0, -140.0), "rz": const(16.0)},
        "leftArm": {"rx": sine(34.0, freq, 0.5, -140.0), "rz": const(-16.0)},
        "rightLeg": {"rx": sine(26.0, freq, 0.5, 8.0), "rz": const(14.0)},
        "leftLeg": {"rx": sine(26.0, freq, 0.0, 8.0), "rz": const(-14.0)},
        "head": {"rx": const(30.0), "ry": sine(10.0, freq * 0.5)},
        "body": {"ry": sine(7.0, freq)},
    }


# ---------------------------------------------------------------------------
# Magic and roleplay
# ---------------------------------------------------------------------------


def cast_spell(style="channel", freq=1.2):
    if style == "burst":
        return {
            "rightArm": {"rx": seq([(0.0, -40.0), (0.4, -168.0), (0.55, -92.0), (0.99, -40.0)]),
                         "rz": seq([(0.0, 14.0), (0.4, 10.0), (0.55, 30.0), (0.99, 14.0)])},
            "leftArm": {"rx": seq([(0.0, -40.0), (0.4, -168.0), (0.55, -92.0), (0.99, -40.0)]),
                        "rz": seq([(0.0, -14.0), (0.4, -10.0), (0.55, -30.0), (0.99, -14.0)])},
            "head": {"rx": seq([(0.0, -8.0), (0.4, -34.0), (0.6, 10.0), (0.99, -8.0)])},
            "body": {"rx": seq([(0.0, 0.0), (0.4, -14.0), (0.6, 12.0), (0.99, 0.0)])},
            "root": {"py": seq([(0.0, 0.0), (0.4, 2.2), (0.6, 0.0), (0.99, 0.0)])},
        }
    if style == "summon":
        return {
            "rightArm": {"rx": add(const(-104.0), sine(8.0, freq)), "rz": add(const(28.0), sine(10.0, freq))},
            "leftArm": {"rx": add(const(-104.0), sine(8.0, freq, 0.5)),
                        "rz": add(const(-28.0), sine(10.0, freq, 0.5))},
            "head": {"rx": const(-20.0), "ry": sine(8.0, freq * 0.5)},
            "body": {"rx": const(-6.0), "ry": sine(6.0, freq * 0.5)},
            "root": {"py": add(const(1.6), sine(1.1, freq * 0.5)), "ry": sine(10.0, freq * 0.25)},
        }
    return {
        "rightArm": {"rx": add(const(-96.0), sine(6.0, freq)), "rz": const(20.0)},
        "leftArm": {"rx": add(const(-96.0), sine(6.0, freq, 0.5)), "rz": const(-20.0)},
        "head": {"rx": const(-14.0)},
        "body": {"rx": const(-5.0), "ry": sine(4.0, freq * 0.5)},
        "root": {"py": add(const(0.8), sine(0.8, freq))},
    }


def archer(freq=0.5, draw=True):
    if draw:
        return {
            "leftArm": {"rx": const(-92.0), "rz": const(-16.0)},
            "rightArm": {"rx": seq([(0.0, -40.0), (0.35, -84.0), (0.7, -86.0), (0.9, -40.0)]),
                         "rz": seq([(0.0, 10.0), (0.35, -34.0), (0.7, -34.0), (0.9, 10.0)])},
            "body": {"ry": const(-26.0)},
            "head": {"ry": const(22.0), "rx": const(-4.0)},
            "rightLeg": {"rz": const(9.0), "ry": const(-14.0)},
            "leftLeg": {"rz": const(-9.0), "ry": const(-6.0)},
        }
    return {
        "leftArm": {"rx": add(const(-92.0), sine(2.0, freq)), "rz": const(-16.0)},
        "rightArm": {"rx": const(-30.0), "rz": const(12.0)},
        "body": {"ry": const(-22.0)},
        "head": {"ry": const(20.0)},
    }


def sword_flourish(freq=1.0):
    return {
        "rightArm": {"rx": seq([(0.0, -34.0), (0.25, -168.0), (0.5, -60.0), (0.75, -110.0), (0.95, -34.0)]),
                     "rz": seq([(0.0, 12.0), (0.25, 22.0), (0.5, 44.0), (0.75, -18.0), (0.95, 12.0)]),
                     "ry": seq([(0.0, 0.0), (0.5, -34.0), (0.95, 0.0)])},
        "leftArm": {"rx": const(-16.0), "rz": const(-30.0)},
        "body": {"ry": seq([(0.0, 0.0), (0.25, -16.0), (0.6, 18.0), (0.95, 0.0)])},
        "head": {"ry": seq([(0.0, 0.0), (0.25, 14.0), (0.6, -14.0), (0.95, 0.0)])},
        "root": {"py": bounce(0.9, freq)},
        "rightLeg": {"rx": const(-10.0)},
        "leftLeg": {"rx": const(8.0)},
    }


def shield_brace(freq=0.5):
    return {
        "leftArm": {"rx": add(const(-84.0), sine(3.0, freq)), "rz": const(-30.0)},
        "rightArm": {"rx": const(-24.0), "rz": const(24.0)},
        "body": {"rx": const(12.0), "ry": const(16.0)},
        "head": {"rx": const(6.0), "ry": const(-12.0)},
        "rightLeg": {"rx": const(14.0), "rz": const(9.0)},
        "leftLeg": {"rx": const(-16.0), "rz": const(-9.0)},
        "root": {"py": const(-1.2)},
    }


def fishing(freq=0.4):
    return {
        "rightArm": {"rx": add(const(-70.0), sine(4.0, freq)), "rz": const(-16.0)},
        "leftArm": {"rx": const(-44.0), "rz": const(-30.0)},
        "head": {"rx": const(8.0), "ry": sine(7.0, freq * 0.5)},
        "body": {"rx": const(5.0), "ry": const(-8.0)},
        "root": {"py": sine(0.3, freq)},
    }


def toast(freq=0.6):
    return {
        "rightArm": {"rx": seq([(0.0, -30.0), (0.3, -122.0), (0.6, -118.0), (0.9, -30.0)]),
                     "rz": seq([(0.0, 10.0), (0.3, -20.0), (0.9, 10.0)])},
        "leftArm": {"rz": const(-12.0)},
        "head": {"rx": seq([(0.0, 0.0), (0.3, -18.0), (0.6, -14.0), (0.9, 0.0)])},
        "body": {"rx": seq([(0.0, 0.0), (0.3, -6.0), (0.9, 0.0)])},
        "root": {"py": seq([(0.0, 0.0), (0.3, 0.8), (0.9, 0.0)])},
    }


def camera_photo(freq=0.5):
    return {
        "rightArm": {"rx": const(-118.0), "rz": const(-26.0)},
        "leftArm": {"rx": const(-118.0), "rz": const(26.0)},
        "head": {"rx": const(-6.0), "ry": sine(5.0, freq)},
        "body": {"rx": const(-3.0)},
        "root": {"py": sine(0.3, freq)},
    }


def dust_off_shoulder(freq=1.2):
    return {
        "rightArm": {"rx": add(const(-108.0), sine(14.0, freq)), "rz": const(-40.0)},
        "leftArm": {"rz": const(-10.0)},
        "head": {"ry": const(-20.0), "rx": const(-10.0)},
        "body": {"ry": const(-12.0)},
        "root": {"py": sine(0.4, freq * 0.5)},
    }


def slow_clap(freq=0.8):
    return clap(freq=freq, height=-66.0, spread=42.0)


def golf_swing(freq=0.5):
    return {
        "rightArm": {"rx": seq([(0.0, -22.0), (0.35, -132.0), (0.5, -30.0), (0.7, -108.0), (0.95, -22.0)]),
                     "rz": seq([(0.0, -20.0), (0.35, -44.0), (0.5, 6.0), (0.95, -20.0)])},
        "leftArm": {"rx": seq([(0.0, -22.0), (0.35, -120.0), (0.5, -26.0), (0.7, -96.0), (0.95, -22.0)]),
                    "rz": seq([(0.0, 20.0), (0.35, -16.0), (0.5, -30.0), (0.95, 20.0)])},
        "body": {"ry": seq([(0.0, -14.0), (0.35, -46.0), (0.55, 34.0), (0.95, -14.0)]), "rx": const(16.0)},
        "head": {"rx": const(16.0), "ry": seq([(0.0, 10.0), (0.35, 30.0), (0.55, -20.0), (0.95, 10.0)])},
        "rightLeg": {"rz": const(9.0)},
        "leftLeg": {"rz": const(-9.0)},
    }


def basketball_shot(freq=0.6):
    return {
        "rightArm": {"rx": seq([(0.0, -46.0), (0.4, -146.0), (0.55, -172.0), (0.9, -46.0)]),
                     "rz": seq([(0.0, 16.0), (0.4, 10.0), (0.9, 16.0)])},
        "leftArm": {"rx": seq([(0.0, -46.0), (0.4, -132.0), (0.55, -150.0), (0.9, -46.0)]),
                    "rz": seq([(0.0, -16.0), (0.4, -10.0), (0.9, -16.0)])},
        "rightLeg": {"rx": seq([(0.0, 0.0), (0.3, -30.0), (0.55, 6.0), (0.9, 0.0)])},
        "leftLeg": {"rx": seq([(0.0, 0.0), (0.3, -30.0), (0.55, 6.0), (0.9, 0.0)])},
        "root": {"py": seq([(0.0, 0.0), (0.3, -2.4), (0.55, 4.6), (0.9, 0.0)])},
        "head": {"rx": seq([(0.0, -8.0), (0.5, -26.0), (0.9, -8.0)])},
        "body": {"rx": seq([(0.0, 0.0), (0.3, 14.0), (0.55, -10.0), (0.9, 0.0)])},
    }
