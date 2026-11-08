import type { MotionOptions, MotionController } from "./motion-runtime";
export type TransferGlowOptions = MotionOptions & { open?: boolean; amount?: string; account?: string; recipient?: string; receivedAmount?: string; duration?: number; onComplete?: () => void; onOpenChange?: (open: boolean) => void; /** 本项目适配：用户点击"确认执行"时触发（业务请求与动效并行） */ onConfirm?: () => void };
export function mountTransferGlow(target: HTMLElement, options?: TransferGlowOptions): Omit<MotionController, "update"> & { update(options: TransferGlowOptions): void };
