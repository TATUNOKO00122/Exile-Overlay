import os
import re

def normalize(name):
    name = re.sub(r'[-_][0-9]+\.[0-9]+.*\.jar$', '', name)
    name = re.sub(r'[-_]fabric[-_].*\.jar$', '', name)
    name = re.sub(r'[-_]forge[-_].*\.jar$', '', name)
    name = re.sub(r'[-_]v?\d+\.\d+\.\d+.*\.jar$', '', name)
    return name.lower().replace('.jar', '')

dir1 = 'F:/ATLauncher/instances/CrafttoExile2VRSupport/mods'
dir2 = 'F:/ATLauncher/instances/Minecraft1201withForgeEEE/mods'

mods1 = {f for f in os.listdir(dir1) if f.endswith('.jar')}
mods2 = {f for f in os.listdir(dir2) if f.endswith('.jar')}

s1 = {normalize(f): f for f in mods1}
s2 = {normalize(f): f for f in mods2}

only_in_1 = [s1[k] for k in set(s1.keys()) - set(s2.keys())]
only_in_2 = [s2[k] for k in set(s2.keys()) - set(s1.keys())]

with open('compare_output.txt', 'w', encoding='utf-8') as f:
    f.write('=== Only in VR Support ===\n')
    f.write('\n'.join(sorted(only_in_1)))
    f.write('\n\n=== Only in Normal ===\n')
    f.write('\n'.join(sorted(only_in_2)))
