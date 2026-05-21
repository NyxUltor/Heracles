"""Fuzzy matching utilities for exercise names."""

def fuzzy_match(query, target):
    """
    Simple fuzzy matching: check if query characters appear in target in order.
    Returns a score (0-100). Higher is better match.
    """
    query = query.lower().strip()
    target = target.lower().strip()

    if not query:
        return 0
    if query == target:
        return 100

    query_idx = 0
    target_idx = 0
    matches = 0

    while query_idx < len(query) and target_idx < len(target):
        if query[query_idx] == target[target_idx]:
            matches += 1
            query_idx += 1
        target_idx += 1

    if query_idx != len(query):
        return 0  # Not all chars matched

    # Score based on how many chars matched and closeness
    score = (matches / len(query)) * 100
    # Bonus for prefix match
    if target.startswith(query):
        score = 100
    # Penalty for long gaps
    chars_checked = target_idx
    if chars_checked > 0:
        score = max(0, score - ((chars_checked - len(query)) * 2))

    return int(max(0, min(100, score)))


def fuzzy_search(query, candidates):
    """
    Search candidates for fuzzy matches.
    Returns sorted list of (score, candidate) tuples, best matches first.
    """
    if not query or not candidates:
        return []

    results = []
    for candidate in candidates:
        score = fuzzy_match(query, candidate)
        if score > 0:
            results.append((score, candidate))

    return sorted(results, key=lambda x: (-x[0], x[1]))  # Sort by score desc, then alphabetically
