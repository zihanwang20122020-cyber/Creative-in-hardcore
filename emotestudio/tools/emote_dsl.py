"""Curve helpers used to author the built-in emotes.

Every emote is described as a set of continuous curves over a normalised phase ``u`` in ``[0, 1)``,
then sampled into keyframes. Authoring in curves rather than raw keyframes is what keeps two hundred
emotes both detailed and maintainable: a wave is "the arm holds up while the forearm oscillates",
not forty hand-typed numbers.

Units match the mod's file format: rotations in degrees, offsets in model pixels (1/16 block),
scales as multipliers. +Y is up.
"""

import math

TAU = 2.0 * math.pi

# The nine animatable channels, in the order the mod reads them.
CHANNELS = ("rx", "ry", "rz", "px", "py", "pz", "sx", "sy", "sz")
DEFAULTS = {"rx": 0.0, "ry": 0.0, "rz": 0.0,
            "px": 0.0, "py": 0.0, "pz": 0.0,
            "sx": 1.0, "sy": 1.0, "sz": 1.0}


def const(value):
    """A channel that never changes."""
    return lambda u: value


def _cycles(freq):
    """Snaps a frequency to a whole number of cycles per emote.

    ``freq`` is expressed in cycles across the whole emote, so only whole numbers land back on the
    starting value at the end. Anything else leaves a step at the loop point that shows up as a
    twitch once per cycle. Archetypes are free to write ``freq * 0.5`` for a half-rate layer; it is
    rounded here rather than every caller having to think about it.
    """
    return max(1, int(round(freq)))


def sine(amp, freq=1.0, phase=0.0, offset=0.0):
    """``offset + amp * sin(2pi * (freq * u + phase))``."""
    cycles = _cycles(freq)
    return lambda u: offset + amp * math.sin(TAU * (cycles * u + phase))


def cosine(amp, freq=1.0, phase=0.0, offset=0.0):
    cycles = _cycles(freq)
    return lambda u: offset + amp * math.cos(TAU * (cycles * u + phase))


def triangle(amp, freq=1.0, phase=0.0, offset=0.0):
    """Linear up/down ramp — reads as a mechanical motion next to a sine."""
    cycles = _cycles(freq)

    def fn(u):
        x = (cycles * u + phase) % 1.0
        return offset + amp * (4.0 * abs(x - 0.5) - 1.0) * -1.0
    return fn


def sawtooth(amp, freq=1.0, phase=0.0, offset=0.0):
    cycles = _cycles(freq)

    def fn(u):
        x = (cycles * u + phase) % 1.0
        return offset + amp * (2.0 * x - 1.0)
    return fn


def pulse(amp, freq=1.0, phase=0.0, duty=0.5, offset=0.0):
    cycles = _cycles(freq)

    def fn(u):
        x = (cycles * u + phase) % 1.0
        return offset + (amp if x < duty else 0.0)
    return fn


def bounce(amp, freq=1.0, phase=0.0, offset=0.0):
    """Absolute sine: always positive, so it reads as a hop rather than a sway."""
    cycles = _cycles(freq)
    return lambda u: offset + amp * abs(math.sin(math.pi * (cycles * u + phase)))


def seq(points, wrap=True):
    """Piecewise-linear curve through ``(u, value)`` points.

    Points must be sorted by ``u``. With ``wrap`` the curve closes back onto the first point, which
    is what a looping emote needs; without it the value holds at each end.
    """
    pts = sorted(points, key=lambda p: p[0])

    def fn(u):
        u = u % 1.0
        if u <= pts[0][0]:
            if not wrap or len(pts) < 2:
                return pts[0][1]
            first, last = pts[0], pts[-1]
            span = (1.0 - last[0]) + first[0]
            if span <= 1e-6:
                return first[1]
            t = (u + (1.0 - last[0])) / span
            return last[1] + (first[1] - last[1]) * t
        if u >= pts[-1][0]:
            if not wrap or len(pts) < 2:
                return pts[-1][1]
            first, last = pts[0], pts[-1]
            span = (1.0 - last[0]) + first[0]
            if span <= 1e-6:
                return last[1]
            t = (u - last[0]) / span
            return last[1] + (first[1] - last[1]) * t
        for i in range(len(pts) - 1):
            a, b = pts[i], pts[i + 1]
            if a[0] <= u <= b[0]:
                span = b[0] - a[0]
                if span <= 1e-6:
                    return b[1]
                t = (u - a[0]) / span
                # Smoothstep between authored poses so choreography does not look robotic.
                t = t * t * (3.0 - 2.0 * t)
                return a[1] + (b[1] - a[1]) * t
        return pts[-1][1]

    return fn


def steps(points):
    """Like :func:`seq` but holds each value until the next point — deliberately robotic."""
    pts = sorted(points, key=lambda p: p[0])

    def fn(u):
        u = u % 1.0
        value = pts[-1][1]
        for at, v in pts:
            if u >= at:
                value = v
        return value

    return fn


def add(*fns):
    """Sums curves, so a sway can be layered on top of a pose."""
    resolved = [f if callable(f) else const(f) for f in fns]
    return lambda u: sum(f(u) for f in resolved)


def scaled(fn, factor):
    inner = fn if callable(fn) else const(fn)
    return lambda u: inner(u) * factor


def shifted(fn, delta):
    """Phase-shifts a curve, the usual way to make a left limb mirror a right one."""
    inner = fn if callable(fn) else const(fn)
    return lambda u: inner((u + delta) % 1.0)


def ramp(start, end):
    """Smooth one-way travel from ``start`` to ``end`` across the whole emote."""
    return lambda u: start + (end - start) * (u * u * (3.0 - 2.0 * u))


def hold_then(value_start, value_end, at):
    """Holds ``value_start`` until ``at``, then eases to ``value_end``."""
    return seq([(0.0, value_start), (at, value_start), (1.0, value_end)], wrap=False)


def turn(total_degrees):
    """Continuous rotation across the emote, without wrapping back through zero.

    Full turns end on exactly ``total_degrees``; 360 and 0 are the same orientation, so a looping
    emote closes seamlessly instead of unwinding backwards on the last frame.
    """
    return lambda u: total_degrees * u


def sample_track(channels, samples, length, loop):
    """Turns a dict of channel curves into the mod's keyframe list.

    Looping emotes get one extra keyframe at exactly ``length``. That closing frame is what makes
    the cycle join up: without it the sampler has to interpolate from the last frame back to the
    first across the leftover slice, which reads as a hitch once per loop.
    """
    frames = []
    count = samples + 1 if loop else samples
    for i in range(count):
        u = (i / samples) if loop else (i / max(1, samples - 1))
        frame = {"t": round(u * length, 3)}
        rot = [_value(channels, "rx", u), _value(channels, "ry", u), _value(channels, "rz", u)]
        pos = [_value(channels, "px", u), _value(channels, "py", u), _value(channels, "pz", u)]
        scl = [_value(channels, "sx", u), _value(channels, "sy", u), _value(channels, "sz", u)]
        if any(abs(v) > 1e-4 for v in rot):
            frame["rot"] = [round(v, 3) for v in rot]
        if any(abs(v) > 1e-4 for v in pos):
            frame["pos"] = [round(v, 3) for v in pos]
        if any(abs(v - 1.0) > 1e-4 for v in scl):
            frame["scale"] = [round(v, 3) for v in scl]
        frames.append(frame)
    return frames


def _value(channels, key, u):
    fn = channels.get(key)
    if fn is None:
        return DEFAULTS[key]
    if callable(fn):
        return float(fn(u))
    return float(fn)
