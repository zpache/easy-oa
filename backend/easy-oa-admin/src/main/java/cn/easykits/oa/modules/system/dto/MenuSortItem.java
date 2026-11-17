package cn.easykits.oa.modules.system.dto;

/**
 * 菜单拖拽排序的单项：目标上级 + 同级顺序。
 * <p>
 * 拖拽可能同时改变父级（拖到某个节点内部），因此 pid 与 order 一起提交。
 */
public record MenuSortItem(Long id, Long pid, Integer order) {
}
