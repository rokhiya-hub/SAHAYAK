import { motion } from 'framer-motion';

const networkNodes = [
  { x: 54, y: 64, label: 'Shelters', tone: '#2E5EFF' },
  { x: 180, y: 110, label: 'Volunteers', tone: '#1FAE6B' },
  { x: 112, y: 220, label: 'Relief', tone: '#FF6B4A' },
  { x: 262, y: 185, label: 'Reports', tone: '#F5A623' },
  { x: 330, y: 82, label: 'Field Ops', tone: '#2E5EFF' },
];

const networkLines = [
  [0, 1],
  [0, 2],
  [0, 3],
  [1, 3],
  [1, 4],
  [2, 3],
  [2, 4],
  [3, 4],
];

export function NetworkHero() {
  return (
    <div className="relative flex w-full max-w-[620px] items-center justify-center">
      <div className="absolute inset-6 rounded-[32px] bg-gradient-to-br from-blue-100/70 via-white to-emerald-100/70 blur-2xl" />
      <motion.div
        initial={{ opacity: 0, scale: 0.96 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.55 }}
        className="relative w-full rounded-[30px] border border-white/70 bg-white/70 p-4 shadow-[0_25px_80px_rgba(46,94,255,0.12)] backdrop-blur-md"
      >
        <svg viewBox="0 0 420 300" className="h-[320px] w-full overflow-visible">
          <defs>
            <linearGradient id="heroLine" x1="0" x2="1">
              <stop offset="0%" stopColor="#2E5EFF" stopOpacity="0.25" />
              <stop offset="50%" stopColor="#2E5EFF" stopOpacity="0.8" />
              <stop offset="100%" stopColor="#1FAE6B" stopOpacity="0.7" />
            </linearGradient>
          </defs>

        {networkLines.map(([a, b], index) => {
          const from = networkNodes[a];
          const to = networkNodes[b];
          return (
            <motion.path
              key={`${a}-${b}`}
              d={`M ${from.x} ${from.y} L ${to.x} ${to.y}`}
              stroke="url(#heroLine)"
              strokeWidth="2.2"
              strokeLinecap="round"
              strokeDasharray="8 10"
              initial={{ pathLength: 0.25, opacity: 0.15 }}
              animate={{ pathLength: 1, opacity: 1 }}
              transition={{ duration: 1.2, ease: 'easeInOut', delay: index * 0.14 }}
            />
          );
        })}

        {networkNodes.map((node, index) => (
          <motion.g
            key={node.label}
            initial={{ scale: 0.8, opacity: 0.4 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ duration: 0.4, delay: index * 0.12 }}
          >
            <circle cx={node.x} cy={node.y} r={20} fill="white" stroke={node.tone} strokeWidth="2" />
            <circle cx={node.x} cy={node.y} r={10} fill={node.tone} opacity={0.22} />
            <circle cx={node.x} cy={node.y} r={5} fill={node.tone} />
            <motion.circle
              cx={node.x}
              cy={node.y}
              r={18}
              fill="none"
              stroke={node.tone}
              strokeWidth="1.5"
              animate={{ opacity: [0.7, 1, 0.7], scale: [1, 1.25, 1] }}
              transition={{ duration: 2.2, repeat: Infinity, ease: 'easeInOut', delay: index * 0.2 }}
            />
          </motion.g>
        ))}

          <motion.g initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.6 }}>
            <rect x="286" y="48" width="90" height="34" rx="14" fill="rgba(46,94,255,0.08)" stroke="rgba(46,94,255,0.35)" />
            <text x="301" y="69" fontSize="12" fill="#141B2D" fontWeight="600">LIVE</text>
          </motion.g>
        </svg>
      </motion.div>
    </div>
  );
}
